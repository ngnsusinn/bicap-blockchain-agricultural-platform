package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.config.SepayConfig;
import vn.courses.ut.edu.javaprogramming.bicap.dto.CreateDepositRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DepositResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.OrderResponse;
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

@Service
@Transactional
public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEPOSIT_CODE_DIGITS = 6;

    private static final Set<String> DEPOSITABLE_STATUSES =
            Set.of(Order.STATUS_PENDING, Order.STATUS_ACCEPTED);

    private static final Set<String> FARM_MANAGER_ROLES = Set.of("FARM_MANAGER");

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

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getRetailerId() == null || !order.getRetailerId().equals(actor.getId())) {
            throw new ForbiddenException("This order does not belong to the current user");
        }

        if (!DEPOSITABLE_STATUSES.contains(order.getStatus())) {
            throw new BadRequestException("Order is not in a valid state for deposit (current: " + order.getStatus() + ")");
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

    private User requireFarmManager() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, FARM_MANAGER_ROLES);
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
