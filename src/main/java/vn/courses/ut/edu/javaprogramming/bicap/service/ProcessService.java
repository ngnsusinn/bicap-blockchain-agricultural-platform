package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ProcessUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingProcessRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;

import java.util.List;

@Service
@Transactional
public class ProcessService {

    private final FarmingProcessRepository processRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmRepository farmRepository;
    private final BlockchainService blockchainService;

    public ProcessService(FarmingProcessRepository processRepository,
                          FarmingSeasonRepository seasonRepository,
                          FarmRepository farmRepository,
                          BlockchainService blockchainService) {
        this.processRepository = processRepository;
        this.seasonRepository = seasonRepository;
        this.farmRepository = farmRepository;
        this.blockchainService = blockchainService;
    }

    public FarmingProcess addProcess(Long seasonId, ProcessCreateRequest request, User currentUser) {
        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));

        Farm farm = farmRepository.findById(season.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + season.getFarmId()));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        if (!"IN_PROGRESS".equals(season.getStatus())) {
            throw new BadRequestException("Season must be in progress");
        }

        FarmingProcess process = new FarmingProcess();
        process.setSeasonId(season.getId());
        process.setProcessType(request.getProcessType());
        process.setExecutionDate(request.getExecutionDate());
        process.setMaterials(request.getMaterials());
        process.setImages(request.getImages());
        process.setNotes(request.getNotes());

        FarmingProcess saved = processRepository.save(process);
        blockchainService.recordProcess(saved);

        return saved;
    }

    public FarmingProcess updateProcess(Long seasonId, Long processId, ProcessUpdateRequest request, User currentUser) {
        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));

        Farm farm = farmRepository.findById(season.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + season.getFarmId()));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        if (!"IN_PROGRESS".equals(season.getStatus())) {
            throw new BadRequestException("Season must be in progress");
        }

        FarmingProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Process not found: " + processId));

        if (!process.getSeasonId().equals(seasonId)) {
            throw new BadRequestException("Process does not belong to this season");
        }

        if (request.getProcessType() != null) process.setProcessType(request.getProcessType());
        if (request.getExecutionDate() != null) process.setExecutionDate(request.getExecutionDate());
        if (request.getMaterials() != null) process.setMaterials(request.getMaterials());
        if (request.getImages() != null) process.setImages(request.getImages());
        if (request.getNotes() != null) process.setNotes(request.getNotes());

        FarmingProcess saved = processRepository.save(process);
        blockchainService.recordProcess(saved);

        return saved;
    }

    public FarmingProcess getProcess(Long seasonId, Long processId, User currentUser) {
        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));

        Farm farm = farmRepository.findById(season.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + season.getFarmId()));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        FarmingProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Process not found: " + processId));

        if (!process.getSeasonId().equals(seasonId)) {
            throw new BadRequestException("Process does not belong to this season");
        }

        return process;
    }

    public List<FarmingProcess> getProcessesBySeason(Long seasonId, User currentUser) {
        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));

        Farm farm = farmRepository.findById(season.getFarmId())
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + season.getFarmId()));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        return processRepository.findBySeasonId(seasonId);
    }
}
