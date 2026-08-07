package vn.courses.ut.edu.javaprogramming.bicap.service;

<<<<<<< HEAD
import org.springframework.stereotype.Service;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Blockchain integration point for VeChainThor (BICAP-14 / BICAP-15).
 *
 * ⚠️  INTEGRATION POINT — NOT YET CONNECTED TO VECHAIN
 * This service defines the contract that FarmingSeasonService relies on.
 * When the VeChain SDK / connector is available, replace the stub body of
 * each method with real VeChainThor calls:
 *
 *   writeSeason()  → FarmingSeasonContract.createSeason(farmId, seasonId, ...)
 *   writeProcess() → FarmingProcessContract.addProcess(seasonId, processId, ...)
 *
 * The return value is a transaction hash (0x-prefixed, 66 chars on VeChain).
 * Until the real integration is wired, the method returns a deterministic
 * placeholder hash so the rest of the system (DB, UI) can operate end-to-end.
 *
 * See docs/detail-design.md §2.9 (BlockchainService) and §5.1/5.2 (Smart Contracts)
 * for the full integration specification.
 */
@Service
public class BlockchainService {

    private static final Logger log = Logger.getLogger(BlockchainService.class.getName());

    /**
     * Writes a farming season to the VeChainThor Blockchain.
     * (BICAP-14 / SRS-FM-008 / FarmingSeasonContract.createSeason)
     *
     * @param season the persisted FarmingSeason entity
     * @return VeChainThor transaction hash (0x-prefixed, 66 chars)
     */
    public String writeSeason(FarmingSeason season) {
        // ── INTEGRATION POINT ──────────────────────────────────────────────
        // TODO: replace with real VeChainThor call when SDK is available:
        //
        //   ThorConnection thor = ThorConnection.connect(nodeUrl);
        //   FarmingSeasonContract contract = thor.load(contractAddress, ABI);
        //   TransactionReceipt receipt = contract.createSeason(
        //       toBytes32(season.getFarmId()),
        //       toBytes32(season.getId()),
        //       season.getName(),
        //       season.getProductType(),
        //       season.getVariety(),
        //       (long)(season.getArea() * 100),
        //       season.getStartDate().toEpochDay()
        //   ).send();
        //   return receipt.getTransactionHash();
        // ───────────────────────────────────────────────────────────────────

        String placeholderHash = "0x" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 30);

        log.info("[BLOCKCHAIN-STUB] writeSeason seasonId=" + season.getId()
                + " farmId=" + season.getFarmId()
                + " → txHash=" + placeholderHash);

        return placeholderHash.substring(0, 66);
    }

    /**
     * Writes a farming process step to the VeChainThor Blockchain.
     * (BICAP-15 / SRS-FM-009 / FarmingProcessContract.addProcess)
     *
     * @param process the persisted FarmingProcess entity
     * @return VeChainThor transaction hash (0x-prefixed, 66 chars)
     */
    public String writeProcess(FarmingProcess process) {
        // ── INTEGRATION POINT ──────────────────────────────────────────────
        // TODO: replace with real VeChainThor call when SDK is available:
        //
        //   TransactionReceipt receipt = contract.addProcess(
        //       toBytes32(process.getSeasonId()),
        //       toBytes32(process.getId()),
        //       process.getProcessType(),
        //       process.getExecutionDate().toEpochDay(),
        //       keccak256(process.getMaterials()),
        //       keccak256(process.getImages())
        //   ).send();
        //   return receipt.getTransactionHash();
        // ───────────────────────────────────────────────────────────────────

        String placeholderHash = "0x" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 30);

        log.info("[BLOCKCHAIN-STUB] writeProcess processId=" + process.getId()
                + " seasonId=" + process.getSeasonId()
                + " type=" + process.getProcessType()
                + " → txHash=" + placeholderHash);

        return placeholderHash.substring(0, 66);
=======
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.courses.ut.edu.javaprogramming.bicap.entity.BlockchainTransaction;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Export;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingProcess;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SmartContract;
import vn.courses.ut.edu.javaprogramming.bicap.repository.BlockchainTransactionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ExportRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingProcessRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.SmartContractRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BlockchainService {

    private final BlockchainTransactionRepository txRepository;
    private final SmartContractRepository contractRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmingProcessRepository processRepository;
    private final ExportRepository exportRepository;
    private final RestTemplate restTemplate;

    @Value("${blockchain.mode:mock}")
    private String mode;

    @Value("${blockchain.node-url:https://node-testnet.vechain.dev}")
    private String nodeUrl;

    @Value("${blockchain.private-key:}")
    private String privateKey;

    public BlockchainService(BlockchainTransactionRepository txRepository,
                             SmartContractRepository contractRepository,
                             FarmingSeasonRepository seasonRepository,
                             FarmingProcessRepository processRepository,
                             ExportRepository exportRepository) {
        this.txRepository = txRepository;
        this.contractRepository = contractRepository;
        this.seasonRepository = seasonRepository;
        this.processRepository = processRepository;
        this.exportRepository = exportRepository;
        this.restTemplate = new RestTemplate();
    }

    public List<SmartContract> getContracts() {
        return contractRepository.findAll();
    }

    public SmartContract deployContract(String name, String bytecode, String abi, String environment, String version) {
        SmartContract contract = new SmartContract();
        contract.setName(name);
        contract.setBytecode(bytecode);
        contract.setAbi(abi);
        contract.setEnvironment(environment != null ? environment : "TESTNET");
        contract.setVersion(version != null ? version : "1.0.0");
        contract.setStatus("PENDING");

        contract = contractRepository.save(contract);

        String txHash = generateMockTxHash();
        String contractAddress = generateMockAddress();

        boolean success = false;
        if ("live".equalsIgnoreCase(mode)) {
            try {
                // Call VeChainThor node endpoint to get block info or simulate deploy
                Map<?, ?> response = restTemplate.getForObject(nodeUrl + "/blocks/best", Map.class);
                if (response != null) {
                    success = true;
                }
            } catch (Exception e) {
                // Fail deployment on connection error to activate retry/failure state
                contract.setStatus("FAILED");
                contractRepository.save(contract);
                throw new RuntimeException("Failed to deploy contract via VeChainThor API: " + e.getMessage(), e);
            }
        } else {
            success = true;
        }

        if (success) {
            contract.setTxHash(txHash);
            contract.setAddress(contractAddress);
            contract.setStatus("ACTIVE");
            contractRepository.save(contract);

            // Record transaction
            BlockchainTransaction tx = new BlockchainTransaction();
            tx.setEntityType("CONTRACT");
            tx.setEntityId(contract.getId());
            tx.setTxHash(txHash);
            tx.setContractAddress(contractAddress);
            tx.setStatus("CONFIRMED");
            tx.setIdempotencyKey("CONTRACT_" + contract.getId());
            txRepository.save(tx);
        }

        return contract;
    }

    public String recordSeason(FarmingSeason season) {
        String idempotencyKey = "SEASON_" + season.getId();
        Optional<BlockchainTransaction> existingTx = txRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            return existingTx.get().getTxHash();
        }

        String txHash = generateMockTxHash();
        boolean success = executeBlockchainCall("SEASON", season.getId());

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setEntityType("SEASON");
        tx.setEntityId(season.getId());
        tx.setTxHash(txHash);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(success ? "CONFIRMED" : "FAILED");
        txRepository.save(tx);

        if (success) {
            season.setTxHash(txHash);
            seasonRepository.save(season);
        }

        return txHash;
    }

    public String recordProcess(FarmingProcess process) {
        String idempotencyKey = "PROCESS_" + process.getId();
        Optional<BlockchainTransaction> existingTx = txRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            return existingTx.get().getTxHash();
        }

        String txHash = generateMockTxHash();
        boolean success = executeBlockchainCall("PROCESS", process.getId());

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setEntityType("PROCESS");
        tx.setEntityId(process.getId());
        tx.setTxHash(txHash);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(success ? "CONFIRMED" : "FAILED");
        txRepository.save(tx);

        if (success) {
            process.setTxHash(txHash);
            processRepository.save(process);
        }

        return txHash;
    }

    public String recordExport(Export export) {
        String idempotencyKey = "EXPORT_" + export.getId();
        Optional<BlockchainTransaction> existingTx = txRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            return existingTx.get().getTxHash();
        }

        String txHash = generateMockTxHash();
        boolean success = executeBlockchainCall("EXPORT", export.getId());

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setEntityType("EXPORT");
        tx.setEntityId(export.getId());
        tx.setTxHash(txHash);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus(success ? "CONFIRMED" : "FAILED");
        txRepository.save(tx);

        if (success) {
            export.setTxHash(txHash);
            exportRepository.save(export);
        }

        return txHash;
    }

    public boolean retryTransaction(Long txId) {
        BlockchainTransaction tx = txRepository.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));

        if ("CONFIRMED".equals(tx.getStatus())) {
            return true;
        }

        tx.setRetryCount(tx.getRetryCount() + 1);

        boolean success = executeBlockchainCall(tx.getEntityType(), tx.getEntityId());
        if (success) {
            tx.setStatus("CONFIRMED");
            updateEntityTxHash(tx.getEntityType(), tx.getEntityId(), tx.getTxHash());
        } else if (tx.getRetryCount() >= 3) {
            tx.setStatus("FAILED");
        }

        txRepository.save(tx);
        return success;
    }

    private boolean executeBlockchainCall(String entityType, Long entityId) {
        if ("live".equalsIgnoreCase(mode)) {
            try {
                // Call VeChainThor node endpoint to get block info or simulate broadcast
                Map<?, ?> response = restTemplate.getForObject(nodeUrl + "/blocks/best", Map.class);
                return response != null;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private void updateEntityTxHash(String entityType, Long entityId, String txHash) {
        if ("SEASON".equalsIgnoreCase(entityType)) {
            seasonRepository.findById(entityId).ifPresent(s -> {
                s.setTxHash(txHash);
                seasonRepository.save(s);
            });
        } else if ("PROCESS".equalsIgnoreCase(entityType)) {
            processRepository.findById(entityId).ifPresent(p -> {
                p.setTxHash(txHash);
                processRepository.save(p);
            });
        } else if ("EXPORT".equalsIgnoreCase(entityType)) {
            exportRepository.findById(entityId).ifPresent(e -> {
                e.setTxHash(txHash);
                exportRepository.save(e);
            });
        }
    }

    private String generateMockTxHash() {
        return "0x" + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String generateMockAddress() {
        String uuidPart1 = UUID.randomUUID().toString().replace("-", "");
        String uuidPart2 = UUID.randomUUID().toString().replace("-", "");
        return "0x" + (uuidPart1 + uuidPart2).substring(0, 40);
>>>>>>> origin/main
    }
}
