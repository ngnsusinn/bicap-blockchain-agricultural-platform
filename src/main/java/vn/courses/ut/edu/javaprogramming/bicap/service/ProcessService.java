package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingProcessRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProcessService {

    private final FarmingProcessRepository processRepository;
    private final BlockchainService blockchainService;

    public ProcessService(FarmingProcessRepository processRepository, BlockchainService blockchainService) {
        this.processRepository = processRepository;
        this.blockchainService = blockchainService;
    }

    public FarmingProcess addProcess(FarmingProcess process) {
        FarmingProcess saved = processRepository.save(process);
        blockchainService.recordProcess(saved);
        return saved;
    }

    public FarmingProcess updateProcess(Long id, FarmingProcess updated) {
        FarmingProcess existing = processRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Farming process not found: " + id));
        existing.setProcessType(updated.getProcessType());
        existing.setExecutionDate(updated.getExecutionDate());
        existing.setMaterials(updated.getMaterials());
        existing.setImages(updated.getImages());
        existing.setNotes(updated.getNotes());

        FarmingProcess saved = processRepository.save(existing);
        blockchainService.recordProcess(saved);
        return saved;
    }

    public Optional<FarmingProcess> getProcess(Long id) {
        return processRepository.findById(id);
    }

    public List<FarmingProcess> getProcessesBySeason(Long seasonId) {
        return processRepository.findBySeasonId(seasonId);
    }
}
