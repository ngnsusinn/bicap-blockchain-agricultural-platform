package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SeasonService {

    private final FarmingSeasonRepository seasonRepository;
    private final BlockchainService blockchainService;

    public SeasonService(FarmingSeasonRepository seasonRepository, BlockchainService blockchainService) {
        this.seasonRepository = seasonRepository;
        this.blockchainService = blockchainService;
    }

    public FarmingSeason createSeason(FarmingSeason season) {
        FarmingSeason saved = seasonRepository.save(season);
        blockchainService.recordSeason(saved);
        return saved;
    }

    public FarmingSeason updateSeason(Long id, FarmingSeason updated) {
        FarmingSeason existing = seasonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Season not found: " + id));
        existing.setName(updated.getName());
        existing.setProductType(updated.getProductType());
        existing.setVariety(updated.getVariety());
        existing.setArea(updated.getArea());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setStatus(updated.getStatus());

        FarmingSeason saved = seasonRepository.save(existing);
        blockchainService.recordSeason(saved);
        return saved;
    }

    public Optional<FarmingSeason> getSeason(Long id) {
        return seasonRepository.findById(id);
    }

    public List<FarmingSeason> getSeasonsByFarm(Long farmId) {
        return seasonRepository.findByFarmId(farmId);
    }
}
