package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmRegistrationRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.FarmUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Lets a Farm Manager register a new farm from the farm portal (H-7). The farm is
 * created in PENDING status and flows through the existing admin approval workflow
 * (BICAP-3). This closes the gap where farm registration never produced a {@code farms} row.
 *
 * <p>Also handles BICAP-9 / SRS-FM-003: updating farm details and (re)uploading the
 * business license / certification documents owned by the farm.
 */
@Service
@Transactional
public class FarmRegistrationService {

    private static final Set<String> FARM_MANAGER_ROLES = Set.of("FARM_MANAGER");

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
    @Transactional(readOnly = true)
    public List<Farm> getMyFarms() {
        User actor = CurrentUser.get();
        return farmRepository.findByUserId(actor.getId());
    }

    /** Loads a farm and asserts it belongs to the authenticated Farm Manager (BICAP-9). */
    @Transactional(readOnly = true)
    public Farm getOwnedFarm(Long farmId) {
        User actor = requireFarmManager();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));
        if (!farm.getUserId().equals(actor.getId())) {
            throw new ForbiddenException("Farm does not belong to current user");
        }
        return farm;
    }

    /**
     * Updates farm details (BICAP-9 / SRS-FM-003). Renaming to a name already taken by
     * another farm is rejected. Editing an already-APPROVED farm keeps its status; the
     * admin still sees the latest data via BICAP-4.
     */
    public Farm updateFarm(Long farmId, FarmUpdateRequest request) {
        Farm farm = getOwnedFarm(farmId);

        String newName = request.getName().trim();
        if (!newName.equals(farm.getName())) {
            farmRepository.findByName(newName).ifPresent(existing -> {
                if (!existing.getId().equals(farm.getId())) {
                    throw new ConflictException("A farm with this name is already registered");
                }
            });
            farm.setName(newName);
        }

        farm.setAddress(request.getAddress().trim());
        farm.setArea(request.getArea());
        farm.setGpsLat(request.getGpsLat());
        farm.setGpsLng(request.getGpsLng());
        farm.setDescription(trimToNull(request.getDescription()));
        farm.setProductTypes(trimToNull(request.getProductTypes()));
        return farmRepository.save(farm);
    }

    /**
     * Uploads/replaces a certification or business-license document for the farm
     * (BICAP-9 / SRS-FM-003). Stored as a {@link FarmCertification} row so the admin
     * approval workflow (BICAP-3) and the farm's certificate tab both see it.
     */
    public FarmCertification addCertification(Long farmId, String type, LocalDate expiryDate,
                                              org.springframework.web.multipart.MultipartFile file) {
        Farm farm = getOwnedFarm(farmId);
        if (file == null || file.isEmpty()) {
            throw new ConflictException("Certification file is required");
        }
        String certType = (type == null || type.isBlank()) ? "BUSINESS_LICENSE" : type.trim().toUpperCase();
        String url = fileStorage.storeBusinessLicense(farm.getUserId(), file);

        FarmCertification cert = FarmCertification.builder()
                .farmId(farm.getId())
                .type(certType)
                .fileUrl(url)
                .expiryDate(expiryDate != null ? expiryDate : LocalDate.now().plusYears(1))
                .build();
        return certificationRepository.save(cert);
    }

    /** Lists a farm's certification / license documents (BICAP-9, farm certificate tab). */
    @Transactional(readOnly = true)
    public List<FarmCertification> getCertifications(Long farmId) {
        Farm farm = getOwnedFarm(farmId);
        return certificationRepository.findByFarmId(farm.getId());
    }

    private User requireFarmManager() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, FARM_MANAGER_ROLES);
        return actor;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
