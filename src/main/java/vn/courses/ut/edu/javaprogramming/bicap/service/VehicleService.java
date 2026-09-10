package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.VehicleRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.VehicleResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.VehicleRepository;

import java.util.List;
import java.util.Set;

/**
 * CRUD for vehicles — restricted to SHIPPING_MGR role (BICAP-76).
 */
@Service
@Transactional
public class VehicleService {

    private static final Set<String> SHIPPING_MGR_ROLES = Set.of("SHIPPING_MGR");

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehicles() {
        requireShippingMgr();
        return vehicleRepository.findAll().stream()
                .map(VehicleResponse::from)
                .toList();
    }

    public VehicleResponse createVehicle(VehicleRequest request) {
        requireShippingMgr();
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new ConflictException("License plate already exists: " + request.getLicensePlate());
        }
        Vehicle v = new Vehicle();
        v.setLicensePlate(request.getLicensePlate().trim().toUpperCase());
        v.setType(request.getType().trim());
        v.setCapacity(request.getCapacity());
        v.setStatus(request.getStatus() != null ? request.getStatus() : Vehicle.STATUS_AVAILABLE);
        return VehicleResponse.from(vehicleRepository.save(v));
    }

    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        requireShippingMgr();
        Vehicle v = findVehicle(id);

        if (request.getLicensePlate() != null && !request.getLicensePlate().isBlank()) {
            String plate = request.getLicensePlate().trim().toUpperCase();
            if (!plate.equals(v.getLicensePlate()) && vehicleRepository.existsByLicensePlate(plate)) {
                throw new ConflictException("License plate already exists: " + plate);
            }
            v.setLicensePlate(plate);
        }
        if (request.getType() != null && !request.getType().isBlank()) {
            v.setType(request.getType().trim());
        }
        if (request.getCapacity() != null) {
            v.setCapacity(request.getCapacity());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            v.setStatus(request.getStatus());
        }
        return VehicleResponse.from(vehicleRepository.save(v));
    }

    public void deleteVehicle(Long id) {
        requireShippingMgr();
        Vehicle v = findVehicle(id);
        if (Vehicle.STATUS_IN_USE.equals(v.getStatus())) {
            throw new BadRequestException("Cannot delete a vehicle that is currently IN_USE");
        }
        vehicleRepository.delete(v);
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id));
    }

    private void requireShippingMgr() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, SHIPPING_MGR_ROLES);
    }
}
