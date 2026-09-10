package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CreateDepositRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DepositResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CancelOrderRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.PlaceOrderRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEPOSIT_CODE_DIGITS = 6;

    private static final Set<String> DEPOSITABLE_STATUSES =
            Set.of(Order.STATUS_PENDING, Order.STATUS_ACCEPTED);

    /** Các trạng thái Retailer được phép hủy đơn (BICAP-44). */
    private static final Set<String> CANCELLABLE_STATUSES =
            Set.of(Order.STATUS_PENDING, Order.STATUS_ACCEPTED);

    private static final Set<String> FARM_MANAGER_ROLES = Set.of("FARM_MANAGER");
    private static final Set<String> RETAILER_ROLES     = Set.of("RETAILER");

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final SepayConfig sepayConfig;
    private final ProductRepository productRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmRepository farmRepository;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, SepayConfig sepayConfig,
                        ProductRepository productRepository, FarmingSeasonRepository seasonRepository,
                        FarmRepository farmRepository, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.sepayConfig = sepayConfig;
        this.productRepository = productRepository;
        this.seasonRepository = seasonRepository;
        this.farmRepository = farmRepository;
        this.notificationService = notificationService;
    }

    /**
     * Creates a deposit transfer memo for an order the CURRENT user owns (H-3 — no more
     * IDOR: any authenticated user can no longer read another party's deposit details).
     * The memo code and expected amount are persisted on the order so the webhook can
     * verify and dedup them (M-8).
     */
    public DepositResponse createDeposit(CreateDepositRequest request, String actorEmail) {
        User actor = ActorAuthorizer.requireActor(userRepository, actorEmail);
        return createDepositFor(request, actor);
    }

    public DepositResponse createDeposit(CreateDepositRequest request) {
        return createDepositFor(request, requireRetailer());
    }

    private DepositResponse createDepositFor(CreateDepositRequest request, User actor) {

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getRetailerId() == null || !order.getRetailerId().equals(actor.getId())) {
            throw new ForbiddenException("This order does not belong to the current user");
        }

        if (!DEPOSITABLE_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException("Order is not in a valid state for deposit (current: " + order.getStatus() + ")");
        }
        if (!Order.STATUS_ACCEPTED.equals(order.getStatus())) {
            throw new BadRequestException("Order must be accepted before deposit");
        }
        if (order.getAcceptedAt() != null && order.getAcceptedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            order.setStatus(Order.STATUS_CANCELLED);
            order.setCancelledReason("Deposit payment window expired");
            orderRepository.save(order);
            throw new BadRequestException("The 24-hour deposit payment window has expired");
        }

        // Tỷ lệ đặt cọc (30% = 0.3) — computed in BigDecimal, rounded to 2 decimals (no double drift).
        BigDecimal rate = order.getDepositRate() != null
                ? BigDecimal.valueOf(order.getDepositRate())
                : new BigDecimal("0.3");
        BigDecimal totalAmount = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        BigDecimal depositAmount = totalAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        // Sinh mã chuyển khoản cọc dạng DEP{orderId}{6_số_ngẫu_nhiên} — SecureRandom, persisted & unique.
        String depositCode = uniqueDepositCode(order.getId());
        order.setDepositCode(depositCode);
        order.setDepositAmount(depositAmount);
        orderRepository.save(order);

        return new DepositResponse(
                order.getId(),
                depositCode,
                sepayConfig.getBankName(),
                sepayConfig.getAccountNo(),
                depositAmount,
                depositCode
        );
    }

    /** BICAP-43 BR3: accepted orders are cancelled when the 24-hour deposit window expires. */
    @Scheduled(fixedDelayString = "${bicap.orders.deposit-expiry-check-ms:60000}")
    public void cancelExpiredDeposits() {
        List<Order> expired = orderRepository.findByStatusAndAcceptedAtBefore(
                Order.STATUS_ACCEPTED, LocalDateTime.now().minusHours(24));
        expired.forEach(order -> {
            order.setStatus(Order.STATUS_CANCELLED);
            order.setCancelledReason("Deposit payment window expired");
        });
        if (!expired.isEmpty()) orderRepository.saveAll(expired);
    }

    /**
     * Marks an order DEPOSIT_PAID after a verified transfer. State-machine guarded (H-5):
     * only PENDING/ACCEPTED may transition, the amount must meet the persisted deposit,
     * and an already-paid order cannot be re-marked.
     */
    public void markAsDepositPaid(Long orderId, BigDecimal transferAmount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (Order.STATUS_DEPOSIT_PAID.equals(order.getStatus())) {
            throw new ConflictException("Order has already been marked as paid");
        }
        if (!DEPOSITABLE_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException("Order is not in a valid state for deposit payment (current: "
                    + order.getStatus() + ")");
        }

        BigDecimal expected = order.getDepositAmount();
        if (expected == null) {
            throw new BadRequestException("No deposit has been initiated for this order");
        }
        if (transferAmount == null || transferAmount.compareTo(expected) < 0) {
            throw new BadRequestException("Transferred amount is below the required deposit");
        }

        order.setStatus(Order.STATUS_DEPOSIT_PAID);
        orderRepository.save(order);
    }

    // ── BICAP-20 / SRS-FM-014: Farm Manager xử lý yêu cầu mua từ Retailer ──

    /**
     * Yêu cầu mua nông sản trên các nông trại của Farm Manager đang đăng nhập,
     * lọc theo trạng thái (bỏ trống để lấy tất cả). Các entity liên quan (sản phẩm,
     * mùa vụ, nông trại, Retailer) được batch-load — tránh N+1 trên mỗi dòng.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getFarmManagerOrders(String status) {
        User actor = requireFarmManager();
        String normalized = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        List<Order> orders = orderRepository.findFarmManagerOrders(actor.getId(), normalized);
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> products = productRepository.findAllById(
                        orders.stream().map(Order::getProductId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, FarmingSeason> seasons = seasonRepository.findAllById(
                        products.values().stream().map(Product::getSeasonId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(FarmingSeason::getId, s -> s));
        Map<Long, Farm> farms = farmRepository.findAllById(
                        seasons.values().stream().map(FarmingSeason::getFarmId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Farm::getId, f -> f));
        Map<Long, User> retailers = userRepository.findAllById(
                        orders.stream().map(Order::getRetailerId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        return orders.stream()
                .map(o -> {
                    Product p = o.getProductId() != null ? products.get(o.getProductId()) : null;
                    FarmingSeason s = p != null ? seasons.get(p.getSeasonId()) : null;
                    Farm f = s != null ? farms.get(s.getFarmId()) : null;
                    User r = o.getRetailerId() != null ? retailers.get(o.getRetailerId()) : null;
                    return OrderResponse.from(o, p, s, f, r);
                })
                .toList();
    }

    /** Chi tiết yêu cầu mua — Farm Manager chỉ xem được đơn của nông trại mình. */
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long id) {
        User actor = requireFarmManager();
        return buildOrderResponse(loadOwnedOrder(id, actor.getId()));
    }

    /**
     * Chấp nhận yêu cầu mua (PENDING → ACCEPTED) — Retailer được thông báo và có 24h
     * đặt cọc (SRS-FM-014 BR1). Không chấp nhận nếu số lượng yêu cầu &gt; tồn kho (BR2).
     */
    public OrderResponse acceptOrder(Long id) {
        User actor = requireFarmManager();
        OrderContext ctx = loadOwnedOrder(id, actor.getId());
        Order order = ctx.order;
        requirePending(order);

        // BR2: số lượng yêu cầu không được vượt quá số lượng tồn kho của sản phẩm.
        if (order.getQuantity() != null && ctx.product != null && ctx.product.getQuantity() != null
                && order.getQuantity() > ctx.product.getQuantity()) {
            throw new BadRequestException("Requested quantity exceeds available stock (available: "
                    + ctx.product.getQuantity() + ")");
        }

        order.setStatus(Order.STATUS_ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        notificationService.sendNotification(order.getRetailerId(), "SUCCESS",
                "Đơn hàng đã được chấp nhận",
                "Yêu cầu mua \"" + productName(ctx) + "\" (" + order.getQuantity() + " đơn vị) đã được Farm Manager "
                        + "chấp nhận. Vui lòng đặt cọc 30% trong vòng 24h để xác nhận đơn hàng.",
                false);

        return buildOrderResponse(ctx.withOrder(saved));
    }

    /**
     * Từ chối yêu cầu mua (PENDING → REJECTED) — bắt buộc nhập lý do, Retailer được thông báo.
     */
    public OrderResponse rejectOrder(Long id, String reason) {
        User actor = requireFarmManager();
        if (reason == null || reason.trim().isEmpty()) {
            throw new BadRequestException("Rejection reason is required");
        }

        OrderContext ctx = loadOwnedOrder(id, actor.getId());
        Order order = ctx.order;
        requirePending(order);

        order.setStatus(Order.STATUS_REJECTED);
        order.setRejectReason(reason.trim());
        Order saved = orderRepository.save(order);

        notificationService.sendNotification(order.getRetailerId(), "WARNING",
                "Đơn hàng bị từ chối",
                "Yêu cầu mua \"" + productName(ctx) + "\" (" + order.getQuantity() + " đơn vị) đã bị từ chối. "
                        + "Lý do: " + reason.trim(),
                false);

        return buildOrderResponse(ctx.withOrder(saved));
    }

    // ── BICAP-75: Đặt mua, Hủy đơn, Giao hàng, Hoàn thành ──

    /**
     * Retailer đặt mua nông sản — tạo Order mới với status PENDING.
     * Snapshot giá tại thời điểm đặt từ Product hiện tại.
     * Gửi thông báo Farm Manager của nông trại chứa sản phẩm.
     */
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User actor = requireRetailer();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        // Chỉ cho phép đặt mua sản phẩm đang ACTIVE
        if (!"ACTIVE".equals(product.getStatus())) {
            throw new BadRequestException("Product is not available for purchase");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0 || product.getQuantity() == null
                || request.getQuantity() > product.getQuantity()) {
            throw new BadRequestException("Requested quantity exceeds available stock");
        }
        if (request.getProposedPrice() != null && request.getProposedPrice().signum() <= 0) {
            throw new BadRequestException("Proposed price must be greater than 0");
        }
        if (request.getDesiredDeliveryDate() != null && !request.getDesiredDeliveryDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Desired delivery date must be at least tomorrow");
        }

        Order order = new Order();
        order.setProductId(product.getId());
        order.setRetailerId(actor.getId());
        order.setQuantity(request.getQuantity());
        // Null fallbacks keep compatibility for trusted internal callers; HTTP requests validate both fields.
        order.setPrice(request.getProposedPrice() != null ? request.getProposedPrice() : product.getPrice());
        order.setDeliveryAddr(request.getDeliveryAddr());
        order.setDesiredDeliveryDate(request.getDesiredDeliveryDate() != null
                ? request.getDesiredDeliveryDate() : LocalDate.now().plusDays(1));
        order.setNotes(request.getNotes() == null ? null : request.getNotes().trim());
        order.setStatus(Order.STATUS_PENDING);
        order.setDepositRate(0.3);

        Order saved = orderRepository.save(order);

        // Thông báo Farm Manager (tìm qua season→farm)
        FarmingSeason season = product.getSeasonId() != null
                ? seasonRepository.findById(product.getSeasonId()).orElse(null) : null;
        if (season != null && season.getFarmId() != null) {
            farmRepository.findById(season.getFarmId()).ifPresent(farm ->
                notificationService.sendNotification(farm.getUserId(), "INFO",
                        "Yêu cầu mua mới",
                        "Nhà bán lẻ " + actor.getFullName() + " đã đặt mua " + request.getQuantity()
                                + " đơn vị \"" + product.getName() + "\". Vui lòng xem xét và xử lý.",
                        false)
            );
        }

        Farm farm = season != null && season.getFarmId() != null
                ? farmRepository.findById(season.getFarmId()).orElse(null) : null;
        return OrderResponse.from(saved, product, season, farm, actor);
    }

    /**
     * Retailer hủy trực tiếp đơn PENDING/ACCEPTED; đơn DEPOSIT_PAID chuyển sang chờ Admin xử lý.
     */
    public OrderResponse cancelOrder(Long id, CancelOrderRequest request) {
        User actor = requireRetailer();

        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("Cancellation reason is required");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        if (!actor.getId().equals(order.getRetailerId())) {
            throw new ForbiddenException("This order does not belong to the current user");
        }
        boolean requiresAdminReview = Order.STATUS_DEPOSIT_PAID.equals(order.getStatus());
        if (!requiresAdminReview && !CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException(
                    "Order cannot be cancelled in its current state (current: " + order.getStatus() + ")");
        }

        order.setStatus(requiresAdminReview ? Order.STATUS_CANCEL_REQUESTED : Order.STATUS_CANCELLED);
        order.setCancelledReason(request.getReason().trim());
        if (requiresAdminReview) {
            order.setCancelRequestedAt(LocalDateTime.now());
        }
        Order saved = orderRepository.save(order);

        // Thông báo Farm Manager
        Product product = order.getProductId() != null
                ? productRepository.findById(order.getProductId()).orElse(null) : null;
        FarmingSeason season = product != null && product.getSeasonId() != null
                ? seasonRepository.findById(product.getSeasonId()).orElse(null) : null;
        if (season != null && season.getFarmId() != null) {
            farmRepository.findById(season.getFarmId()).ifPresent(farm ->
                notificationService.sendNotification(farm.getUserId(), "WARNING",
                        requiresAdminReview ? "Yêu cầu hủy đơn đã đặt cọc" : "Đơn hàng bị hủy",
                        "Nhà bán lẻ " + actor.getFullName()
                                + (requiresAdminReview ? " yêu cầu hủy đơn hàng #" : " đã hủy đơn hàng #") + id
                                + (order.getCancelledReason() != null ? ". Lý do: " + order.getCancelledReason() : "."),
                        false)
            );
        }

        if (requiresAdminReview) {
            userRepository.findDistinctByRoles_NameIn(Set.of("ADMIN", "SUPER_ADMIN")).forEach(admin ->
                    notificationService.sendNotification(admin.getId(), "WARNING",
                            "Yêu cầu hủy đơn đã đặt cọc",
                            "Nhà bán lẻ " + actor.getFullName() + " yêu cầu hủy đơn #" + id
                                    + ". Lý do: " + order.getCancelledReason(), false));
        }

        Farm farm = season != null && season.getFarmId() != null
                ? farmRepository.findById(season.getFarmId()).orElse(null) : null;
        User retailer = userRepository.findById(actor.getId()).orElse(actor);
        return OrderResponse.from(saved, product, season, farm, retailer);
    }

    /** Đánh dấu đơn đã đặt cọc đang được vận chuyển (điểm tích hợp với shipment module). */
    public OrderResponse markInTransit(Long id) {
        User actor = requireFarmManager();
        OrderContext ctx = loadOwnedOrder(id, actor.getId());
        Order order = ctx.order;
        if (!Order.STATUS_DEPOSIT_PAID.equals(order.getStatus())) {
            throw new BadRequestException(
                    "Only DEPOSIT_PAID orders can enter transit (current: " + order.getStatus() + ")");
        }
        order.setStatus(Order.STATUS_IN_TRANSIT);
        Order saved = orderRepository.save(order);
        notificationService.sendNotification(order.getRetailerId(), "INFO",
                "Đơn hàng đang vận chuyển",
                "Đơn hàng \"" + productName(ctx) + "\" đã bắt đầu vận chuyển.", false);
        return buildOrderResponse(ctx.withOrder(saved));
    }

    /**
     * Farm Manager xác nhận đã giao hàng (IN_TRANSIT → DELIVERED).
     * Gửi thông báo Retailer để xác nhận đã nhận.
     */
    public OrderResponse confirmDelivery(Long id) {
        User actor = requireFarmManager();
        OrderContext ctx = loadOwnedOrder(id, actor.getId());
        Order order = ctx.order;

        if (!Order.STATUS_IN_TRANSIT.equals(order.getStatus())) {
            throw new BadRequestException(
                    "Only IN_TRANSIT orders can be marked as delivered (current: " + order.getStatus() + ")");
        }

        order.setStatus(Order.STATUS_DELIVERED);
        order.setDeliveredAt(java.time.LocalDateTime.now());
        Order saved = orderRepository.save(order);

        notificationService.sendNotification(order.getRetailerId(), "INFO",
                "Đơn hàng đã được giao",
                "Đơn hàng \"" + productName(ctx) + "\" (" + order.getQuantity()
                        + " đơn vị) đã được giao. Vui lòng xác nhận đã nhận hàng.",
                false);

        return buildOrderResponse(ctx.withOrder(saved));
    }

    /**
     * Retailer xác nhận đã nhận hàng (DELIVERED → COMPLETED).
     * Chỉ chủ đơn mới được xác nhận.
     */
    public OrderResponse completeOrder(Long id) {
        User actor = requireRetailer();

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        if (!actor.getId().equals(order.getRetailerId())) {
            throw new ForbiddenException("This order does not belong to the current user");
        }
        if (!Order.STATUS_DELIVERED.equals(order.getStatus())) {
            throw new BadRequestException(
                    "Only DELIVERED orders can be completed (current: " + order.getStatus() + ")");
        }

        order.setStatus(Order.STATUS_COMPLETED);
        order.setCompletedAt(java.time.LocalDateTime.now());
        Order saved = orderRepository.save(order);

        // Thông báo Farm Manager
        Product product = order.getProductId() != null
                ? productRepository.findById(order.getProductId()).orElse(null) : null;
        FarmingSeason season = product != null && product.getSeasonId() != null
                ? seasonRepository.findById(product.getSeasonId()).orElse(null) : null;
        if (season != null && season.getFarmId() != null) {
            farmRepository.findById(season.getFarmId()).ifPresent(farm ->
                notificationService.sendNotification(farm.getUserId(), "SUCCESS",
                        "Đơn hàng hoàn thành",
                        "Nhà bán lẻ " + actor.getFullName() + " đã xác nhận nhận hàng. Đơn hàng #" + id + " đã hoàn thành.",
                        false)
            );
        }

        Farm farm = season != null && season.getFarmId() != null
                ? farmRepository.findById(season.getFarmId()).orElse(null) : null;
        User retailer = userRepository.findById(actor.getId()).orElse(actor);
        return OrderResponse.from(saved, product, season, farm, retailer);
    }

    /**
     * Retailer xem danh sách đơn hàng của mình, lọc theo trạng thái (BICAP-45).
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getRetailerOrders(String status) {
        User actor = requireRetailer();
        String normalized = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        List<Order> orders = orderRepository.findRetailerOrders(actor.getId(), normalized);
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, Product> products = productRepository.findAllById(
                        orders.stream().map(Order::getProductId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, FarmingSeason> seasons = seasonRepository.findAllById(
                        products.values().stream().map(Product::getSeasonId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(FarmingSeason::getId, s -> s));
        Map<Long, Farm> farms = farmRepository.findAllById(
                        seasons.values().stream().map(FarmingSeason::getFarmId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Farm::getId, f -> f));

        return orders.stream()
                .map(o -> {
                    Product p = o.getProductId() != null ? products.get(o.getProductId()) : null;
                    FarmingSeason s = p != null ? seasons.get(p.getSeasonId()) : null;
                    Farm f = s != null ? farms.get(s.getFarmId()) : null;
                    return OrderResponse.from(o, p, s, f, actor);
                })
                .toList();
    }

    /** Retailer xem chi tiết đơn của chính mình (BICAP-46 / SRS-RT-011). */
    @Transactional(readOnly = true)
    public OrderResponse getRetailerOrderDetail(Long id) {
        User actor = requireRetailer();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (!actor.getId().equals(order.getRetailerId())) {
            throw new ForbiddenException("This order does not belong to the current user");
        }

        Product product = order.getProductId() == null
                ? null : productRepository.findById(order.getProductId()).orElse(null);
        FarmingSeason season = product == null || product.getSeasonId() == null
                ? null : seasonRepository.findById(product.getSeasonId()).orElse(null);
        Farm farm = season == null || season.getFarmId() == null
                ? null : farmRepository.findById(season.getFarmId()).orElse(null);
        return OrderResponse.from(order, product, season, farm, actor);
    }

    private User requireFarmManager() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, FARM_MANAGER_ROLES);
        return actor;
    }

    private User requireRetailer() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, RETAILER_ROLES);
        return actor;
    }

    /** Loads an order together with its product/season/farm/retailer and asserts farm ownership. */
    private OrderContext loadOwnedOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found for order: " + orderId));
        FarmingSeason season = product.getSeasonId() != null
                ? seasonRepository.findById(product.getSeasonId()).orElse(null) : null;
        Farm farm = season != null && season.getFarmId() != null
                ? farmRepository.findById(season.getFarmId()).orElse(null) : null;
        if (farm == null || !farm.getUserId().equals(userId)) {
            throw new ForbiddenException("Order does not belong to a farm owned by the current user");
        }

        User retailer = order.getRetailerId() != null
                ? userRepository.findById(order.getRetailerId()).orElse(null) : null;
        return new OrderContext(order, product, season, farm, retailer);
    }

    private void requirePending(Order order) {
        if (!Order.STATUS_PENDING.equals(order.getStatus())) {
            throw new BadRequestException("Only PENDING orders can be processed (current: " + order.getStatus() + ")");
        }
    }

    private String productName(OrderContext ctx) {
        return ctx.product != null ? ctx.product.getName() : "sản phẩm #" + ctx.order.getProductId();
    }

    private OrderResponse buildOrderResponse(OrderContext ctx) {
        return OrderResponse.from(ctx.order, ctx.product, ctx.season, ctx.farm, ctx.retailer);
    }

    /** Immutable context bundled for one order so lookups run once per request (no N+1 within detail). */
    private record OrderContext(Order order, Product product, FarmingSeason season, Farm farm, User retailer) {
        OrderContext withOrder(Order order) {
            return new OrderContext(order, product, season, farm, retailer);
        }
    }

    private String uniqueDepositCode(Long orderId) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = "DEP" + orderId + String.format("%0" + DEPOSIT_CODE_DIGITS + "d", RANDOM.nextInt(1_000_000));
            if (orderRepository.findByDepositCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique deposit code");
    }
}
