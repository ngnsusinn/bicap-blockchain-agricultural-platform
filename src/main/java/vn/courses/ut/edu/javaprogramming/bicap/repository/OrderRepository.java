package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByRetailerId(Long retailerId);
    Optional<Order> findByDepositCode(String depositCode);

    /**
     * Orders placed against products grown on farms owned by {@code userId} (BICAP-20).
     * The farm is resolved through Order → Product → FarmingSeason → Farm via explicit
     * joins on the scalar id columns. Passing a {@code null} status returns all statuses.
     */
    @Query("SELECT o FROM Order o " +
           "JOIN Product p ON o.productId = p.id " +
           "JOIN FarmingSeason s ON p.seasonId = s.id " +
           "JOIN Farm f ON s.farmId = f.id " +
           "WHERE f.userId = :userId " +
           "AND (:status IS NULL OR o.status = :status) " +
           "ORDER BY o.createdAt DESC, o.id DESC")
    List<Order> findFarmManagerOrders(@Param("userId") Long userId, @Param("status") String status);

    /**
     * Orders that a single retailer has placed against products grown on farms owned by
     * {@code userId} (BICAP-21 / SRS-FM-015) — the retailer's transaction history for one
     * Farm Manager. Empty when the retailer has never transacted with the user's farms.
     */
    @Query("SELECT o FROM Order o " +
           "JOIN Product p ON o.productId = p.id " +
           "JOIN FarmingSeason s ON p.seasonId = s.id " +
           "JOIN Farm f ON s.farmId = f.id " +
           "WHERE f.userId = :userId AND o.retailerId = :retailerId " +
           "ORDER BY o.createdAt DESC, o.id DESC")
    List<Order> findFarmManagerRetailerOrders(@Param("userId") Long userId,
                                              @Param("retailerId") Long retailerId);

    /**
     * Orders placed by a single retailer (BICAP-75 — Retailer xem đơn hàng của mình).
     * Passing a {@code null} status returns all statuses.
     */
    @Query("SELECT o FROM Order o " +
           "WHERE o.retailerId = :retailerId " +
           "AND (:status IS NULL OR o.status = :status) " +
           "ORDER BY o.createdAt DESC, o.id DESC")
    List<Order> findRetailerOrders(@Param("retailerId") Long retailerId, @Param("status") String status);
}