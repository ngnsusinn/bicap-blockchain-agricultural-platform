package vn.courses.ut.edu.javaprogramming.bicap.service;

import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;

/**
 * Anchoring boundary for a season export receipt (the record behind the traceability QR).
 *
 * <p>The default implementation is {@link VeChainExportBlockchainGateway}, which routes
 * through {@link BlockchainService}: a real signed VeChainThor broadcast when
 * {@code blockchain.mode=live}, and a clearly-labelled simulated receipt otherwise.
 * {@link LocalExportBlockchainGateway} remains available, but only when
 * {@code bicap.blockchain.export-mode=local} is set explicitly.
 */
public interface ExportBlockchainGateway {

    /** Anchors the export and returns the transaction hash (0x + 64 hex). */
    String recordExport(SeasonExport export);

    /**
     * Whether {@link #recordExport} produced a real on-chain transaction. Callers persist
     * this so the UI can tell the user honestly whether the receipt is LIVE or simulated.
     */
    default boolean isLiveAnchor() {
        return false;
    }
}
