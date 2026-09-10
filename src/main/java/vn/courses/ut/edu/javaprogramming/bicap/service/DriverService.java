package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DriverCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DriverResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DriverUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Driver;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Vehicle;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.DriverRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.VehicleRepository;

import java.util.List;
import java.util.Set;

/**
 * CRUD for driver profiles — restricted to SHIPPING_MGR role (BICAP-76).
 */
@Service
@Transactional
public class DriverService {

    private static final Set<String> SHIPPING_MGR_ROLES = Set.of("SHIPPING_MGR");

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(DriverRepository driverRepository,
                         UserRepository userRepository,
                         VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getDrivers() {
        requireShippingMgr();
        return driverRepository.findAll().stream()
                .map(d -> buildResponse(d))
                .toList();
    }

    public DriverResponse createDriver(DriverCreateRequest request) {
        requireShippingMgr();

        if (driverRepository.existsByUserId(request.getUserId())) {
            throw new ConflictException("User already registered as a driver: " + request.getUserId());
        }
        if (driverRepository.existsByCitizenId(request.getCitizenId())) {
            throw new ConflictException("Citizen ID already exists: " + request.getCitizenId());
        }
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ConflictException("License number already exists: " + request.getLicenseNumber());
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        // Validate vehicle if provided
        if (request.getVehicleId() != null) {
            Vehicle v = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.getVehicleId()));
            if (!Vehicle.STATUS_AVAILABLE.equals(v.getStatus())) {
                throw new BadRequestException("Vehicle is not AVAILABLE: " + request.getVehicleId());
            }
        }

        Driver d = new Driver();
        d.setUserId(request.getUserId());
        d.setCitizenId(request.getCitizenId().trim());
        d.setLicenseNumber(request.getLicenseNumber().trim());
        d.setVehicleId(request.getVehicleId());
        Driver saved = driverRepository.save(d);

        Vehicle vehicle = saved.getVehicleId() != null
                ? vehicleRepository.findById(saved.getVehicleId()).orElse(null) : null;
        return DriverResponse.from(saved, user, vehicle);
    }

    public DriverResponse updateDriver(Long id, DriverUpdateRequest request) {
        requireShippingMgr();
        Driver d = findDriver(id);

        if (request.getCitizenId() != null && !request.getCitizenId().isBlank()) {
            d.setCitizenId(request.getCitizenId().trim());
        }
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().isBlank()) {
            d.setLicenseNumber(request.getLicenseNumber().trim());
        }
        if (request.getVehicleId() != null) {
            vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.getVehicleId()));
            d.setVehicleId(request.getVehicleId());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            d.setStatus(request.getStatus());
        }
        return buildResponse(driverRepository.save(d));
    }

    public void deleteDriver(Long id) {
        requireShippingMgr();
        Driver d = findDriver(id);
        if (Driver.STATUS_ON_TRIP.equals(d.getStatus())) {
            throw new BadRequestException("Cannot delete a driver that is currently ON_TRIP");
        }
        driverRepository.delete(d);
    }

    /** Assign a vehicle to a driver — BR2: driver must be IDLE, BR3: vehicle must be AVAILABLE. */
    public DriverResponse assignVehicle(Long driverId, Long vehicleId) {
        requireShippingMgr();
        Driver d = findDriver(driverId);
        if (!Driver.STATUS_IDLE.equals(d.getStatus())) {
            throw new BadRequestException("Driver must be IDLE to assign a vehicle (current: " + d.getStatus() + ")");
        }
        Vehicle v = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
        if (!Vehicle.STATUS_AVAILABLE.equals(v.getStatus())) {
            throw new BadRequestException("Vehicle must be AVAILABLE to be assigned (current: " + v.getStatus() + ")");
        }
        d.setVehicleId(vehicleId);
        return buildResponse(driverRepository.save(d));
    }

    private DriverResponse buildResponse(Driver d) {
        User user = d.getUserId() != null
                ? userRepository.findById(d.getUserId()).orElse(null) : null;
        Vehicle vehicle = d.getVehicleId() != null
                ? vehicleRepository.findById(d.getVehicleId()).orElse(null) : null;
        return DriverResponse.from(d, user, vehicle);
    }

    private Driver findDriver(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + id));
    }

    private void requireShippingMgr() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, SHIPPING_MGR_ROLES);
    }
}
