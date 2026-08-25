package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Shipment;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;

import java.util.List;

/**
 * Full detail response for a single shipment — extends the list summary with
 * a complete GPS tracking history (BICAP-76).
 */
public class ShipmentDetailResponse extends ShipmentResponse {

    private List<TrackingResponse> trackingHistory;

    public ShipmentDetailResponse() {}

    public static ShipmentDetailResponse fromDetail(Shipment s, Order order,
                                                    Driver driver, User driverUser,
                                                    Vehicle vehicle,
                                                    List<TrackingResponse> tracking) {
        ShipmentDetailResponse r = new ShipmentDetailResponse();
        ShipmentResponse base = ShipmentResponse.from(s, order, driver, driverUser, vehicle);
        r.setId(base.getId());
        r.setStatus(base.getStatus());
        r.setCreatedAt(base.getCreatedAt());
        r.setPickupTime(base.getPickupTime());
        r.setDeliveryTime(base.getDeliveryTime());
        r.setRouteSummary(base.getRouteSummary());
        r.setOrderId(base.getOrderId());
        r.setDeliveryAddr(base.getDeliveryAddr());
        r.setDriverId(base.getDriverId());
        r.setDriverName(base.getDriverName());
        r.setDriverPhone(base.getDriverPhone());
        r.setVehicleId(base.getVehicleId());
        r.setVehicleLicensePlate(base.getVehicleLicensePlate());
        r.setVehicleType(base.getVehicleType());
        r.trackingHistory = tracking;
        return r;
    }

    public List<TrackingResponse> getTrackingHistory() { return trackingHistory; }
    public void setTrackingHistory(List<TrackingResponse> trackingHistory) { this.trackingHistory = trackingHistory; }
}
