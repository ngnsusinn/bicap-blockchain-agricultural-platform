package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmRegistrationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Lets a Farm Manager register a new farm from the farm portal (H-7). The farm is
 * created in PENDING status and flows through the existing admin approval workflow
 * (BICAP-3). This closes the gap where farm registration never produced a {@code farms} row.
 */
@Service
@Transactional
public class FarmRegistrationService {

    private final FarmRepository farmRepository;
    private final FarmCertificationRepository certificationRepository;
    private final LocalFileStorageService fileStorage;

    public FarmRegistrationService(FarmRepository farmRepository,
                                   FarmCertificationRepository certificationRepository,
                                   LocalFileStorageService fileStorage) {
        this.farmRepository = farmRepository;
        this.certificationRepository = certificationRepository;
        this.fileStorage = fileStorage;
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

    /**
     * Updates only the current user's farm. A re-submitted registration must return
     * to the existing admin approval queue (SRS-FM-003), without exposing admin-only fields.
     */
    public Farm updateMyFarm(Long farmId, FarmUpdateRequest request) {
        User actor = CurrentUser.get();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + farmId));
        if (!farm.getUserId().equals(actor.getId())) {
            throw new ForbiddenException("You do not have access to this farm");
        }
        String name = request.getName().trim();
        farmRepository.findByName(name).filter(other -> !other.getId().equals(farmId))
                .ifPresent(other -> { throw new ConflictException("A farm with this name is already registered"); });

        farm.setName(name);
        farm.setAddress(request.getAddress().trim());
        farm.setArea(request.getArea());
        farm.setGpsLat(request.getGpsLat());
        farm.setGpsLng(request.getGpsLng());
        farm.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        farm.setProductTypes(request.getProductTypes().trim());
        farm.setStatus(FarmStatus.PENDING);
        return farmRepository.save(farm);
    }

    public Farm registerFarm(FarmRegistrationRequest request, MultipartFile businessLicense) {
        return registerFarm(request, businessLicense, List.of());
    }

    public Farm registerFarm(FarmRegistrationRequest request, MultipartFile businessLicense,
                             List<MultipartFile> certifications) {
        if (businessLicense == null || businessLicense.isEmpty()) {
            throw new vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException(
                    "Business license is required");
        }
        Farm farm = registerFarm(request);
        String fileUrl = fileStorage.storeFarmBusinessLicense(CurrentUser.get().getId(), businessLicense);
        certificationRepository.save(vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification.builder()
                .farmId(farm.getId())
                .type("BUSINESS_LICENSE")
                .fileUrl(fileUrl)
                .expiryDate(LocalDate.of(9999, 12, 31))
                .build());
        if (certifications != null) {
            for (MultipartFile certification : certifications) {
                if (certification == null || certification.isEmpty()) continue;
                certificationRepository.save(vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification.builder()
                        .farmId(farm.getId())
                        .type("CERTIFICATION")
                        .fileUrl(fileStorage.storeFarmCertification(CurrentUser.get().getId(), certification))
                        .expiryDate(LocalDate.of(9999, 12, 31))
                        .build());
            }
        }
        return farm;
    }
}
