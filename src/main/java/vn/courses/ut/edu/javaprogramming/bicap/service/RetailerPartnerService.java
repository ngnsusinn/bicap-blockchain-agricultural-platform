package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RetailerPartnerDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RetailerPartnerResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RetailerTransactionResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.entity.RetailerBusinessProfile;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.OrderRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RetailerBusinessProfileRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Xem thông tin Nhà bán lẻ đã ký hợp đồng (BICAP-21 / SRS-FM-015): danh sách đối tác
 * và chi tiết kèm lịch sử giao dịch để Farm Manager đánh giá đối tác.
 *
 * <p>Một Retailer được xem là "đã ký hợp đồng" với Farm Manager khi có ít nhất một
 * đơn hàng đặt trên sản phẩm của nông trại họ sở hữu (liên kết Order → Product →
 * FarmingSeason → Farm). Tất cả truy vấn đều bị phạm vi về farm của user đang đăng nhập.
 */
@Service
@Transactional(readOnly = true)
public class RetailerPartnerService {

    private static final Set<String> FARM_MANAGER_ROLES = Set.of("FARM_MANAGER");

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RetailerBusinessProfileRepository businessProfileRepository;
    private final ProductRepository productRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmRepository farmRepository;

    public RetailerPartnerService(OrderRepository orderRepository, UserRepository userRepository,
                                  RetailerBusinessProfileRepository businessProfileRepository,
                                  ProductRepository productRepository, FarmingSeasonRepository seasonRepository,
                                  FarmRepository farmRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.productRepository = productRepository;
        this.seasonRepository = seasonRepository;
        this.farmRepository = farmRepository;
    }

    /**
     * Danh sách Nhà bán lẻ đã có đơn hàng (hợp đồng) trên các nông trại của Farm Manager
     * đang đăng nhập, kèm chỉ số giao dịch tổng hợp (số đơn, tổng giá trị, lần đầu/cuối).
     * Retailer &amp; hồ sơ kinh doanh được batch-load — tránh N+1 trên mỗi dòng.
     * Sắp xếp theo giao dịch gần nhất trước.
     */
    public List<RetailerPartnerResponse> getContractRetailers() {
        User actor = requireFarmManager();
        List<Order> orders = orderRepository.findFarmManagerOrders(actor.getId(), null);
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Order>> ordersByRetailer = orders.stream()
                .filter(o -> o.getRetailerId() != null)
                .collect(Collectors.groupingBy(Order::getRetailerId));

        Map<Long, User> retailers = userRepository.findAllById(ordersByRetailer.keySet())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, RetailerBusinessProfile> profiles = businessProfileRepository
                .findByUserIdIn(ordersByRetailer.keySet())
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        return ordersByRetailer.entrySet().stream()
                .map(e -> buildSummary(e.getKey(), e.getValue(), retailers.get(e.getKey()), profiles.get(e.getKey())))
                .sorted(Comparator.comparing(RetailerPartnerResponse::getLastOrderAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Chi tiết một Nhà bán lẻ đã ký hợp đồng với nông trại của Farm Manager: thông tin
     * tổng hợp kèm lịch sử giao dịch (đơn hàng + sản phẩm + nông trại). Nếu Retailer
     * chưa từng đặt hàng trên nông trại của user → {@link ResourceNotFoundException}
     * (không phải đối tác của user này).
     */
    public RetailerPartnerDetailResponse getContractRetailerDetail(Long retailerId) {
        User actor = requireFarmManager();
        List<Order> orders = orderRepository.findFarmManagerRetailerOrders(actor.getId(), retailerId);
        if (orders.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Retailer has no contracts with the current user's farms: " + retailerId);
        }

        User retailer = userRepository.findById(retailerId).orElse(null);
        RetailerBusinessProfile profile = businessProfileRepository.findByUserId(retailerId).orElse(null);
        RetailerPartnerResponse summary = buildSummary(retailerId, orders, retailer, profile);

        Map<Long, Product> products = productRepository.findAllById(
                        orders.stream().map(Order::getProductId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, FarmingSeason> seasons = seasonRepository.findAllById(
                        products.values().stream().map(Product::getSeasonId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(FarmingSeason::getId, s -> s));
        Map<Long, Farm> farms = farmRepository.findAllById(
                        seasons.values().stream().map(FarmingSeason::getFarmId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Farm::getId, f -> f));

        List<RetailerTransactionResponse> transactions = orders.stream()
                .map(o -> {
                    Product p = o.getProductId() != null ? products.get(o.getProductId()) : null;
                    FarmingSeason s = p != null ? seasons.get(p.getSeasonId()) : null;
                    Farm f = s != null ? farms.get(s.getFarmId()) : null;
                    return RetailerTransactionResponse.from(o, p, s, f);
                })
                .toList();

        return new RetailerPartnerDetailResponse(summary, transactions);
    }

    /** Tổng hợp chỉ số giao dịch từ danh sách đơn hàng của một Retailer. */
    private RetailerPartnerResponse buildSummary(Long retailerId, List<Order> orders,
                                                 User retailer, RetailerBusinessProfile profile) {
        long totalOrders = orders.size();
        BigDecimal totalSpent = orders.stream()
                .filter(o -> o.getPrice() != null && o.getQuantity() != null)
                .map(o -> o.getPrice().multiply(BigDecimal.valueOf(o.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime first = orders.stream().map(Order::getCreatedAt).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime last = orders.stream().map(Order::getCreatedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return RetailerPartnerResponse.from(retailerId, retailer, profile, totalOrders, totalSpent, first, last);
    }

    private User requireFarmManager() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, FARM_MANAGER_ROLES);
        return actor;
    }
}
