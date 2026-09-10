package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CurrentUser;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonExportRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.SeasonExportResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.*;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SeasonExportRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class SeasonExportService {
    private final SeasonExportRepository exports;
    private final FarmRepository farms;
    private final FarmingSeasonExportGateway seasons;
    private final ExportBlockchainGateway blockchain;
    private final QrCodeService qrCodes;

    public SeasonExportService(SeasonExportRepository exports, FarmRepository farms,
            FarmingSeasonExportGateway seasons, ExportBlockchainGateway blockchain, QrCodeService qrCodes) {
        this.exports = exports; this.farms = farms; this.seasons = seasons;
        this.blockchain = blockchain; this.qrCodes = qrCodes;
    }

    @Transactional
    public SeasonExportResponse create(Long farmId, Long seasonId, SeasonExportRequest request, String idempotencyKey) {
        User actor = requireFarmManager();
        requireOwnedFarm(farmId, actor.getId());
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100)
            throw new BadRequestException("X-Idempotency-Key is required and must not exceed 100 characters");
        var existing = exports.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            SeasonExport prior = existing.get();
            boolean sameRequest = prior.getFarmId().equals(farmId)
                    && prior.getSeasonId().equals(seasonId)
                    && prior.getQuantity().compareTo(request.quantity()) == 0
                    && prior.getUnit().equalsIgnoreCase(request.unit().trim())
                    && prior.getExportDate().equals(request.exportDate())
                    && prior.getWarehouse().equals(request.warehouse().trim());
            if (!sameRequest) throw new ConflictException("Idempotency key already used for a different export request");
            return SeasonExportResponse.from(existing.get());
        }
        var season = seasons.requireHarvested(farmId, seasonId);
        if (season.harvestUnit() != null && !season.harvestUnit().equalsIgnoreCase(request.unit()))
            throw new BadRequestException("Export unit must match harvest unit: " + season.harvestUnit());
        BigDecimal committed = exports.sumCommittedQuantity(seasonId, ExportStatus.BLOCKCHAIN_FAILED);
        if (committed.add(request.quantity()).compareTo(season.harvestedQuantity()) > 0)
            throw new BadRequestException("Export quantity exceeds remaining harvest quantity");

        SeasonExport value = new SeasonExport();
        value.setFarmId(farmId); value.setSeasonId(seasonId); value.setQuantity(request.quantity());
        value.setUnit(request.unit().trim()); value.setExportDate(request.exportDate());
        value.setWarehouse(request.warehouse().trim()); value.setStatus(ExportStatus.BLOCKCHAIN_PENDING);
        value.setIdempotencyKey(idempotencyKey.trim()); value.setCreatedBy(actor.getId());
        value = exports.saveAndFlush(value);
        try {
            String transactionHash = blockchain.recordExport(value);
            if (transactionHash == null || !transactionHash.matches("0x[0-9a-fA-F]{64}"))
                throw new IllegalStateException("Blockchain returned an invalid transaction hash");
            value.setTransactionHash(transactionHash);
            value.setTraceHash(value.getTransactionHash());
        } catch (RuntimeException ex) {
            value.setStatus(ExportStatus.BLOCKCHAIN_FAILED);
            return SeasonExportResponse.from(exports.save(value));
        }
        try {
            value.setQrImage(qrCodes.pngDataUri(value.getTraceHash()));
            value.setStatus(ExportStatus.READY);
        } catch (RuntimeException ex) {
            value.setStatus(ExportStatus.QR_FAILED);
        }
        return SeasonExportResponse.from(exports.save(value));
    }

    @Transactional(readOnly = true)
    public List<SeasonExportResponse> list(Long farmId) {
        User actor = requireFarmManager(); requireOwnedFarm(farmId, actor.getId());
        return exports.findByFarmIdOrderByCreatedAtDesc(farmId).stream().map(SeasonExportResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SeasonExportResponse trace(String hash) {
        return exports.findByTraceHash(hash).filter(e -> e.getStatus() == ExportStatus.READY)
                .map(SeasonExportResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Traceable export not found"));
    }

    private User requireFarmManager() {
        User actor = CurrentUser.get();
        ActorAuthorizer.requireRoles(actor, Set.of("FARM_MANAGER"));
        return actor;
    }
    private void requireOwnedFarm(Long farmId, Long userId) {
        var farm = farms.findById(farmId).orElseThrow(() -> new ResourceNotFoundException("Farm not found"));
        if (!farm.getUserId().equals(userId)) throw new ForbiddenException("Farm does not belong to current user");
    }
}
