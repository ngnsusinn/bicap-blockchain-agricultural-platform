package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ProcessStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingProcessRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for Farm Season module (BICAP-12/13/14/15 / SRS-FM-006/007/008/009).
 *
 * BICAP-12 — getProcesses    : danh sách quy trình của một mùa vụ
 * BICAP-13 — getSeasonDetail : chi tiết mùa vụ kèm quy trình
 * BICAP-14 — createSeason    : tạo mùa vụ mới + ghi Blockchain
 * BICAP-15 — addProcess      : thêm bước quy trình + ghi Blockchain
 */
@Service
@Transactional
public class FarmingSeasonService {

    private final FarmingSeasonRepository seasonRepository;
    private final FarmingProcessRepository processRepository;
    private final FarmRepository farmRepository;
    private final BlockchainService blockchainService;

    public FarmingSeasonService(FarmingSeasonRepository seasonRepository,
                                FarmingProcessRepository processRepository,
                                FarmRepository farmRepository,
                                BlockchainService blockchainService) {
        this.seasonRepository = seasonRepository;
        this.processRepository = processRepository;
        this.farmRepository = farmRepository;
        this.blockchainService = blockchainService;
    }

    // ── BICAP-12: Xem danh sách quy trình của mùa vụ ─────────────────────────

    /**
     * Lấy danh sách tất cả quy trình theo thứ tự ngày thực hiện (BICAP-12).
     * Chỉ chủ farm mới được xem.
     */
    @Transactional(readOnly = true)
    public List<ProcessResponse> getProcesses(Long farmId, Long seasonId) {
        User actor = CurrentUser.get();
        verifyFarmOwnership(farmId, actor);
        getSeasonAndVerifyBelongsToFarm(seasonId, farmId);

        return processRepository.findBySeasonIdOrderByExecutionDateAsc(seasonId)
                .stream()
                .map(ProcessResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── BICAP-13: Xem chi tiết mùa vụ ────────────────────────────────────────

    /**
     * Lấy danh sách mùa vụ của farm (BICAP-13 — list view).
     * Trả về Page để hỗ trợ phân trang.
     */
    @Transactional(readOnly = true)
    public Page<SeasonResponse> getSeasons(Long farmId, Pageable pageable) {
        User actor = CurrentUser.get();
        verifyFarmOwnership(farmId, actor);

        return seasonRepository.findByFarmId(farmId, pageable).map(s -> {
            SeasonResponse r = SeasonResponse.fromEntity(s);
            r.setProcessCount(processRepository.countBySeasonId(s.getId()));
            return r;
        });
    }

    /**
     * Lấy chi tiết một mùa vụ kèm toàn bộ quy trình (BICAP-13 — detail view).
     */
    @Transactional(readOnly = true)
    public SeasonResponse getSeasonDetail(Long farmId, Long seasonId) {
        User actor = CurrentUser.get();
        verifyFarmOwnership(farmId, actor);
        FarmingSeason season = getSeasonAndVerifyBelongsToFarm(seasonId, farmId);

        SeasonResponse response = SeasonResponse.fromEntity(season);

        List<ProcessResponse> processes = processRepository
                .findBySeasonIdOrderByExecutionDateAsc(seasonId)
                .stream()
                .map(ProcessResponse::fromEntity)
                .collect(Collectors.toList());
        response.setProcesses(processes);
        response.setProcessCount((long) processes.size());

        return response;
    }

    // ── BICAP-14: Tạo mùa vụ mới + ghi Blockchain ────────────────────────────

    /**
     * Tạo mùa vụ mới và ghi lên Blockchain (BICAP-14 / SRS-FM-008).
     * Business rules:
     * - BR3: Farm phải ở trạng thái APPROVED
     * - Sau khi lưu DB, gọi BlockchainService.writeSeason() → lưu txHash
     */
    public SeasonResponse createSeason(Long farmId, SeasonRequest request) {
        User actor = CurrentUser.get();
        Farm farm = verifyFarmOwnership(farmId, actor);

        // BR3: Farm phải APPROVED
        if (farm.getStatus() != vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus.APPROVED) {
            throw new BadRequestException(
                    "Nông trại chưa được Admin phê duyệt. Vui lòng chờ phê duyệt trước khi tạo mùa vụ.");
        }

        // Validate endDate > startDate nếu có
        if (request.getEndDate() != null && !request.getEndDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu.");
        }

        // Lưu vào DB trước
        FarmingSeason season = FarmingSeason.builder()
                .farmId(farmId)
                .name(request.getName().trim())
                .productType(request.getProductType().trim())
                .variety(request.getVariety().trim())
                .area(request.getArea())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(SeasonStatus.IN_PROGRESS)
                .build();
        season.setDescription(request.getDescription());
        season.setNotes(request.getNotes());

        FarmingSeason saved = seasonRepository.save(season);

        // Ghi lên Blockchain (integration point)
        try {
            String txHash = blockchainService.writeSeason(saved);
            saved.setTxHash(txHash);
            saved = seasonRepository.save(saved);
        } catch (Exception e) {
            // E1: Blockchain thất bại → vẫn trả về season với txHash = null
            // Có thể queue retry ở đây khi BlockchainService hoàn chỉnh
        }

        SeasonResponse response = SeasonResponse.fromEntity(saved);
        response.setProcessCount(0L);
        return response;
    }

    // ── BICAP-15: Thêm bước quy trình + ghi Blockchain ───────────────────────

    /**
     * Thêm bước quy trình mới vào mùa vụ và ghi lên Blockchain (BICAP-15 / SRS-FM-009).
     * Business rules:
     * - BR1: Mùa vụ phải ở trạng thái IN_PROGRESS
     * - BR2: Mỗi lần cập nhật tạo một bản ghi mới trên Blockchain (append-only)
     */
    public ProcessResponse addProcess(Long farmId, Long seasonId, ProcessRequest request) {
        User actor = CurrentUser.get();
        verifyFarmOwnership(farmId, actor);
        FarmingSeason season = getSeasonAndVerifyBelongsToFarm(seasonId, farmId);

        // BR1: Chỉ cập nhật được mùa vụ IN_PROGRESS
        if (season.getStatus() != SeasonStatus.IN_PROGRESS) {
            throw new BadRequestException(
                    "Chỉ có thể thêm quy trình cho mùa vụ đang tiến hành (IN_PROGRESS). "
                            + "Trạng thái hiện tại: " + season.getStatus());
        }

        // Lưu vào DB
        FarmingProcess process = FarmingProcess.builder()
                .seasonId(seasonId)
                .processType(request.getProcessType().trim())
                .executionDate(request.getExecutionDate())
                .materials(request.getMaterials())
                .images(request.getImages())
                .notes(request.getNotes())
                .build();
        process.setName(request.getName().trim());
        process.setDescription(request.getDescription());
        process.setPerformedBy(actor.getFullName());
        process.setStatus(ProcessStatus.valueOf(request.getStatus()));

        FarmingProcess saved = processRepository.save(process);

        // Ghi lên Blockchain (integration point) — BR2: append-only
        try {
            String txHash = blockchainService.writeProcess(saved);
            saved.setTxHash(txHash);
            saved = processRepository.save(saved);
        } catch (Exception e) {
            // Blockchain thất bại → vẫn trả về process với txHash = null
        }

        return ProcessResponse.fromEntity(saved);
    }

    /**
     * Updates an existing process step and records the new state through the
     * existing blockchain integration. The database retains the latest
     * operational view while the blockchain receives another append-only write.
     */
    public ProcessResponse updateProcess(Long farmId, Long seasonId, Long processId, ProcessRequest request) {
        User actor = CurrentUser.get();
        verifyFarmOwnership(farmId, actor);
        FarmingSeason season = getSeasonAndVerifyBelongsToFarm(seasonId, farmId);
        if (season.getStatus() != SeasonStatus.IN_PROGRESS) {
            throw new BadRequestException("Chỉ có thể cập nhật quy trình cho mùa vụ đang tiến hành (IN_PROGRESS).");
        }

        FarmingProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Process not found with id: " + processId));
        if (!process.getSeasonId().equals(seasonId)) {
            throw new BadRequestException("Process does not belong to the specified season");
        }

        process.setName(request.getName().trim());
        process.setProcessType(request.getProcessType().trim());
        process.setDescription(request.getDescription());
        process.setExecutionDate(request.getExecutionDate());
        process.setStatus(ProcessStatus.valueOf(request.getStatus()));
        process.setMaterials(request.getMaterials());
        process.setImages(request.getImages());
        process.setNotes(request.getNotes());
        process.setPerformedBy(actor.getFullName());

        FarmingProcess saved = processRepository.save(process);
        try {
            saved.setTxHash(blockchainService.writeProcess(saved));
            saved = processRepository.save(saved);
        } catch (Exception e) {
            // The operational update remains available; blockchain retry can use
            // the persisted process record when the provider becomes available.
        }
        return ProcessResponse.fromEntity(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Xác minh user là chủ của farm. Trả về Farm entity.
     */
    private Farm verifyFarmOwnership(Long farmId, User actor) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with id: " + farmId));
        if (!farm.getUserId().equals(actor.getId())) {
            throw new ForbiddenException("You do not have permission to access this farm");
        }
        return farm;
    }

    /**
     * Lấy season và xác minh nó thuộc farm đã chỉ định.
     */
    private FarmingSeason getSeasonAndVerifyBelongsToFarm(Long seasonId, Long farmId) {
        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id: " + seasonId));
        if (!season.getFarmId().equals(farmId)) {
            throw new BadRequestException("Season does not belong to the specified farm");
        }
        return season;
    }
}
