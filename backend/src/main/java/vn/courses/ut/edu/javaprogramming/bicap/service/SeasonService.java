package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.*;

import java.time.LocalDate;

@Service
@Transactional
public class SeasonService {
    private final FarmingSeasonRepository seasonRepository;
    private final FarmRepository farmRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BlockchainService blockchainService;

    public SeasonService(FarmingSeasonRepository seasonRepository,
                         FarmRepository farmRepository,
                         SubscriptionRepository subscriptionRepository,
                         BlockchainService blockchainService) {
        this.seasonRepository = seasonRepository;
        this.farmRepository = farmRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.blockchainService = blockchainService;
    }

    public FarmingSeason createSeason(Long farmId, SeasonCreateRequest request, User currentUser) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        if (farm.getStatus() != FarmStatus.APPROVED) {
            throw new BadRequestException("Farm must be approved");
        }

        boolean hasActiveSubscription = subscriptionRepository.findByFarmIdAndStatus(farm.getId(), SubscriptionStatus.ACTIVE).isPresent();
        if (!hasActiveSubscription) {
            throw new BadRequestException("Active subscription required");
        }

        FarmingSeason season = new FarmingSeason();
        season.setFarmId(farm.getId());
        season.setName(request.getName());
        season.setProductType(request.getProductType());
        season.setVariety(request.getVariety());
        season.setArea(request.getArea());
        season.setStartDate(request.getStartDate());
        season.setStatus("IN_PROGRESS");

        FarmingSeason saved = seasonRepository.save(season);
        blockchainService.recordSeason(saved);
        
        return saved;
    }

    public FarmingSeason updateSeason(Long farmId, Long seasonId, SeasonUpdateRequest request, User currentUser) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));

        if (!season.getFarmId().equals(farmId)) {
            throw new BadRequestException("Season does not belong to this farm");
        }

        if (request.getName() != null) season.setName(request.getName());
        if (request.getProductType() != null) season.setProductType(request.getProductType());
        if (request.getVariety() != null) season.setVariety(request.getVariety());
        if (request.getArea() != null) season.setArea(request.getArea());
        if (request.getStartDate() != null) season.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) season.setEndDate(request.getEndDate());

        FarmingSeason saved = seasonRepository.save(season);
        blockchainService.recordSeason(saved);

        return saved;
    }

    public FarmingSeason updateSeasonStatus(Long farmId, Long seasonId,
                                            vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonStatusUpdateRequest request,
                                            User currentUser) {
        String newStatus = request.getStatus();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));

        if (!season.getFarmId().equals(farmId)) {
            throw new BadRequestException("Season does not belong to this farm");
        }

        if (!"IN_PROGRESS".equals(season.getStatus())) {
            throw new BadRequestException("Only IN_PROGRESS seasons can change status");
        }

        if (!"HARVESTED".equals(newStatus) && !"CANCELLED".equals(newStatus)) {
            throw new BadRequestException("Invalid status transition");
        }

        season.setStatus(newStatus);
        
        if ("HARVESTED".equals(newStatus)) {
            // BICAP-16: the harvested amount is mandatory so the season can be exported
            // (SeasonExportService validates export quantities against it).
            if (request.getHarvestedQuantity() == null) {
                throw new BadRequestException(
                        "harvestedQuantity is required when marking a season as HARVESTED");
            }
            season.setEndDate(LocalDate.now());
            season.setHarvestedQuantity(request.getHarvestedQuantity());
            season.setHarvestUnit(request.getHarvestUnit() != null && !request.getHarvestUnit().isBlank()
                    ? request.getHarvestUnit().trim() : "kg");
        }

        FarmingSeason saved = seasonRepository.save(season);
        blockchainService.recordSeason(saved);

        return saved;
    }

    public FarmingSeason getSeason(Long farmId, Long seasonId, User currentUser) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        FarmingSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found: " + seasonId));

        if (!season.getFarmId().equals(farmId)) {
            throw new BadRequestException("Season does not belong to this farm");
        }

        return season;
    }

    public Page<FarmingSeason> getSeasonsByFarm(Long farmId, String status, Pageable pageable, User currentUser) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        if (!farm.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not own this farm");
        }

        if (status != null && !status.isEmpty()) {
            return seasonRepository.findByFarmIdAndStatus(farmId, status, pageable);
        } else {
            return seasonRepository.findByFarmId(farmId, pageable);
        }
    }
}
