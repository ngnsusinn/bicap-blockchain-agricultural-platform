package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ShipmentTracking;

import java.util.List;

@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {
    /** Returns all tracking points for a shipment, newest first. */
    List<ShipmentTracking> findByShipmentIdOrderByTimestampDesc(Long shipmentId);

    /**
     * Returns all driver report checkpoints (status starts with REPORT_) visible to
     * Shipping Manager (BICAP-62). Optionally scoped to one shipment when
     * {@code shipmentId} is non-null.
     */
    @Query("SELECT t FROM ShipmentTracking t " +
           "WHERE t.status LIKE 'REPORT_%' " +
           "AND (:shipmentId IS NULL OR t.shipmentId = :shipmentId) " +
           "ORDER BY t.timestamp DESC")
    List<ShipmentTracking> findDriverReports(@Param("shipmentId") Long shipmentId);
}
