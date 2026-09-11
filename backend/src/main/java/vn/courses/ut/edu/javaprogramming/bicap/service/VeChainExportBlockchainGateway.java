package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;

/**
 * Default export anchoring adapter (F1 fix).
 *
 * <p>Routes the export receipt through {@link BlockchainService#recordSeasonExport}, which
 * broadcasts a signed VeChainThor transaction in {@code live} mode and records the
 * transaction in {@code blockchain_transactions}. This makes the export/QR traceability
 * claim real: previously this path was hard-wired to {@link LocalExportBlockchainGateway}
 * (a local SHA-256 hash) even when the platform ran with {@code blockchain.mode=live}.
 */
@Component
@ConditionalOnProperty(name = "bicap.blockchain.export-mode", havingValue = "vechain", matchIfMissing = true)
public class VeChainExportBlockchainGateway implements ExportBlockchainGateway {

    private final BlockchainService blockchainService;

    public VeChainExportBlockchainGateway(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @Override
    public String recordExport(SeasonExport export) {
        return blockchainService.recordSeasonExport(export);
    }

    @Override
    public boolean isLiveAnchor() {
        return blockchainService.isLive();
    }
}
