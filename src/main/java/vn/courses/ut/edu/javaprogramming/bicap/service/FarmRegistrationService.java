package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmRegistrationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;

/**
 * Lets a Farm Manager register a new farm from the farm portal (H-7). The farm is
 * created in PENDING status and flows through the existing admin approval workflow
 * (BICAP-3). This closes the gap where farm registration never produced a {@code farms} row.
 */
@Service
@Transactional
public class FarmRegistrationService {

    private final FarmRepository farmRepository;

    public FarmRegistrationService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    public Farm registerFarm(FarmRegistrationRequest request) {
        User actor = CurrentUser.get();

        if (farmRepository.findByName(request.getName().trim()).isPresent()) {
            throw new ConflictException("A farm with this name is already registered");
        }

        Farm farm = Farm.builder()
                .userId(actor.getId())
                .name(request.getName().trim())
                .address(request.getAddress().trim())
                .area(request.getArea())
                .gpsLat(request.getGpsLat())
                .gpsLng(request.getGpsLng())
                .description(request.getDescription())
                .productTypes(request.getProductTypes())
                .status(FarmStatus.PENDING)
                .build();
        return farmRepository.save(farm);
    }

    /** All farms owned by the authenticated user (drives the farm portal's farmId — M-2). */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<Farm> getMyFarms() {
        User actor = CurrentUser.get();
        return farmRepository.findByUserId(actor.getId());
    }
}
