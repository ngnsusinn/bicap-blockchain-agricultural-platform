package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Export;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ExportRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExportService {

    private final ExportRepository exportRepository;
    private final BlockchainService blockchainService;

    public ExportService(ExportRepository exportRepository, BlockchainService blockchainService) {
        this.exportRepository = exportRepository;
        this.blockchainService = blockchainService;
    }

    public Export createExport(Export export) {
        Export saved = exportRepository.save(export);
        blockchainService.recordExport(saved);
        return saved;
    }

    public Export updateExport(Long id, Export updated) {
        Export existing = exportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Export not found: " + id));
        existing.setExportDate(updated.getExportDate());
        existing.setQuantity(updated.getQuantity());
        existing.setDestination(updated.getDestination());

        Export saved = exportRepository.save(existing);
        blockchainService.recordExport(saved);
        return saved;
    }

    public Optional<Export> getExport(Long id) {
        return exportRepository.findById(id);
    }

    public List<Export> getExportsBySeason(Long seasonId) {
        return exportRepository.findBySeasonId(seasonId);
    }
}
