package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainClient;
import vn.courses.ut.edu.javaprogramming.bicap.entity.BlockchainTransaction;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SmartContract;
import vn.courses.ut.edu.javaprogramming.bicap.repository.BlockchainTransactionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ExportRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingProcessRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SmartContractRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BICAP-80 — blockchain throughput & self-healing.
 *
 * <p>Under load, attestation writes are queued in the {@code blockchain_transactions}
 * table (PENDING) instead of blocking request threads on node round-trips. This job:
 * <ol>
 *   <li><b>confirms</b> PENDING transactions from the node receipt (CONFIRMED / FAILED),
 *       propagating the real tx hash back to the season/process/export/contract record;</li>
 *   <li><b>auto-retries</b> FAILED transactions (up to 3 attempts) and expires PENDING
 *       transactions that never landed (stuck &gt; 30 minutes).</li>
 * </ol>
 * Runs only in {@code blockchain.mode=live}; in mock mode everything confirms inline.
 */
@Component
public class BlockchainMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(BlockchainMaintenanceJob.class);
    private static final int MAX_RETRIES = 3;
    private static final long PENDING_STUCK_MINUTES = 30;

    private final BlockchainService blockchainService;
    private final BlockchainTransactionRepository txRepository;
    private final SmartContractRepository contractRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmingProcessRepository processRepository;
    private final ExportRepository exportRepository;
    private final String nodeUrl;

    public BlockchainMaintenanceJob(BlockchainService blockchainService,
                                    BlockchainTransactionRepository txRepository,
                                    SmartContractRepository contractRepository,
                                    FarmingSeasonRepository seasonRepository,
                                    FarmingProcessRepository processRepository,
                                    ExportRepository exportRepository,
                                    @org.springframework.beans.factory.annotation.Value("${blockchain.node-url:https://testnet.vechain.org}") String nodeUrl) {
        this.blockchainService = blockchainService;
        this.txRepository = txRepository;
        this.contractRepository = contractRepository;
        this.seasonRepository = seasonRepository;
        this.processRepository = processRepository;
        this.exportRepository = exportRepository;
        this.nodeUrl = nodeUrl;
    }

    /** Poll node receipts for PENDING transactions. */
    @Scheduled(fixedDelayString = "${blockchain.confirm-interval-ms:15000}")
    @Transactional
    public void confirmPendingTransactions() {
        if (!blockchainService.isLive()) return;
        VeChainClient node = nodeClient();
        if (node == null) return;
        for (BlockchainTransaction tx : txRepository.findByStatus("PENDING")) {
            try {
                int status = node.getTransactionStatus(tx.getTxHash());
                if (status == VeChainClient.TX_CONFIRMED) {
                    VeChainClient.Receipt receipt = node.getReceipt(tx.getTxHash());
                    if (receipt != null && receipt.reverted()) {
                        markFailed(tx);
                    } else {
                        tx.setStatus("CONFIRMED");
                        txRepository.save(tx);
                        propagateConfirmation(tx);
                    }
                } else if (status == VeChainClient.TX_ERROR) {
                    markFailed(tx);
                }
                // TX_PENDING / TX_UNKNOWN: leave for next tick (expiry handled by retry job)
            } catch (Exception e) {
                log.warn("Confirm check failed for tx {}: {}", tx.getId(), e.getMessage());
            }
        }
    }

    /** Re-broadcast FAILED transactions and expire stuck PENDING ones. */
    @Scheduled(fixedDelayString = "${blockchain.retry-interval-ms:60000}")
    @Transactional
    public void retryFailedTransactions() {
        if (!blockchainService.isLive()) return;

        for (BlockchainTransaction tx : txRepository.findByStatus("FAILED")) {
            if (tx.getRetryCount() >= MAX_RETRIES) continue;
            try {
                blockchainService.retryTransaction(tx.getId());
                log.info("Auto-retried blockchain tx {} (attempt {})", tx.getId(), tx.getRetryCount() + 1);
            } catch (Exception e) {
                log.warn("Auto-retry failed for tx {}: {}", tx.getId(), e.getMessage());
            }
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_STUCK_MINUTES);
        for (BlockchainTransaction tx : txRepository.findByStatus("PENDING")) {
            if (tx.getCreatedAt() != null && tx.getCreatedAt().isBefore(cutoff)) {
                log.warn("Blockchain tx {} stuck PENDING > {}min — marking FAILED for retry",
                        tx.getId(), PENDING_STUCK_MINUTES);
                markFailed(tx);
            }
        }
    }

    private void markFailed(BlockchainTransaction tx) {
        tx.setStatus("FAILED");
        txRepository.save(tx);
    }

    /** On confirmation, write the real tx hash onto the underlying entity. */
    private void propagateConfirmation(BlockchainTransaction tx) {
        switch (tx.getEntityType().toUpperCase()) {
            case "SEASON" -> seasonRepository.findById(tx.getEntityId()).ifPresent(s -> {
                s.setTxHash(tx.getTxHash());
                seasonRepository.save(s);
            });
            case "PROCESS" -> processRepository.findById(tx.getEntityId()).ifPresent(p -> {
                p.setTxHash(tx.getTxHash());
                processRepository.save(p);
            });
            case "EXPORT" -> exportRepository.findById(tx.getEntityId()).ifPresent(e -> {
                e.setTxHash(tx.getTxHash());
                exportRepository.save(e);
            });
            case "CONTRACT" -> contractRepository.findById(tx.getEntityId()).ifPresent(c -> {
                c.setStatus("ACTIVE");
                c.setTxHash(tx.getTxHash());
                contractRepository.save(c);
            });
            default -> { /* QR etc. — hash already on the row */ }
        }
    }

    private VeChainClient nodeClient() {
        try {
            return new VeChainClient(nodeUrl);
        } catch (Exception e) {
            log.warn("Node client unavailable: {}", e.getMessage());
            return null;
        }
    }
}
