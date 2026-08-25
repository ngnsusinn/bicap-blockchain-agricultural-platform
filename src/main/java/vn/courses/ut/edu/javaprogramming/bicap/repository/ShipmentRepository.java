package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByOrderId(Long orderId);

    /**
     * All shipments visible to Shipping Manager, optionally filtered by status.
     * Null status → return all.
     */
    @Query("SELECT s FROM Shipment s " +
           "WHERE (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.createdAt DESC, s.id DESC")
    List<Shipment> findAllFiltered(@Param("status") String status);

    /**
     * Shipments assigned to a specific driver, optionally filtered by status (BICAP-76).
     */
    @Query("SELECT s FROM Shipment s " +
           "WHERE s.driverId = :driverId " +
           "AND (:status IS NULL OR s.status = :status) " +
           "ORDER BY s.createdAt DESC, s.id DESC")
    List<Shipment> findByDriverIdFiltered(@Param("driverId") Long driverId,
                                          @Param("status") String status);
}
