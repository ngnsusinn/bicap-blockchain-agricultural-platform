package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.Hashes;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.HexUtils;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainClient;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainTxSigner;
import vn.courses.ut.edu.javaprogramming.bicap.common.blockchain.VeChainWallet;
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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.math.BigInteger;
import java.util.Optional;

/**
 * Blockchain anchoring for farm records (BICAP-6/14/15/17/74).
 *
 * <p>Two modes:
 * <ul>
 *   <li><b>mock</b> (default, dev/CI): deterministic local hashes, transactions are
 *       immediately CONFIRMED — no network required.</li>
 *   <li><b>live</b>: transactions are RLP-encoded, secp256k1-signed and broadcast to a
 *       real VeChainThor node ({@code blockchain.node-url}) using {@code blockchain.private-key}.
 *       Broadcasted transactions are stored PENDING with the real tx id; the scheduled
 *       {@link BlockchainMaintenanceJob} confirms them from the node receipt and retries
 *       failures automatically (BICAP-80).</li>
 * </ul>
 */
@Service
@Transactional
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private final BlockchainTransactionRepository txRepository;
    private final SmartContractRepository contractRepository;
    private final FarmingSeasonRepository seasonRepository;
    private final FarmingProcessRepository processRepository;
    private final ExportRepository exportRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${blockchain.mode:mock}")
    private String mode;

    @Value("${blockchain.node-url:https://testnet.vechain.org}")
    private String nodeUrl;

    @Value("${blockchain.private-key:}")
    private String privateKey;

    @Value("${blockchain.gas-price-coef:0}")
    private int gasPriceCoef;

    @Value("${blockchain.gas-attest:53000}")
    private long gasAttest;

    @Value("${blockchain.gas-deploy:10000000}")
    private long gasDeploy;

    @Value("${blockchain.expiration:720}")
    private int expiration;

    private volatile VeChainClient client;

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
    }

    public boolean isLive() {
        return "live".equalsIgnoreCase(mode);
    }

    private VeChainClient client() {
        VeChainClient c = client;
        if (c == null) {
            synchronized (this) {
                c = client;
                if (c == null) {
                    c = new VeChainClient(nodeUrl);
                    client = c;
                }
            }
        }
        return c;
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

        if (isLive()) {
            long nonce = randomNonce();
            String sender = walletAddress();
            String predictedAddress = VeChainWallet.contractAddress(sender, nonce);
            String txId = broadcast(List.of(
                    VeChainTxSigner.Clause.create(BigInteger.ZERO, HexUtils.fromHex(bytecode))),
                    gasDeploy, nonce);

            contract.setTxHash(txId);
            contract.setAddress(predictedAddress);
            contract.setStatus("PENDING");
            contractRepository.save(contract);

            BlockchainTransaction tx = new BlockchainTransaction();
            tx.setEntityType("CONTRACT");
            tx.setEntityId(contract.getId());
            tx.setTxHash(txId);
            tx.setContractAddress(predictedAddress);
            tx.setStatus("PENDING");
            tx.setIdempotencyKey("CONTRACT_" + contract.getId());
            txRepository.save(tx);
            return contract;
        }

        String txHash = generateMockTxHash();
        String contractAddress = generateMockAddress();
        contract.setTxHash(txHash);
        contract.setAddress(contractAddress);
        contract.setStatus("ACTIVE");
        contractRepository.save(contract);

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setEntityType("CONTRACT");
        tx.setEntityId(contract.getId());
        tx.setTxHash(txHash);
        tx.setContractAddress(contractAddress);
        tx.setStatus("CONFIRMED");
        tx.setIdempotencyKey("CONTRACT_" + contract.getId());
        txRepository.save(tx);
        return contract;
    }

    public String recordSeason(FarmingSeason season) {
        return recordEntity("SEASON", season.getId(),
                hash -> { season.setTxHash(hash); seasonRepository.save(season); });
    }

    public String recordProcess(FarmingProcess process) {
        return recordEntity("PROCESS", process.getId(),
                hash -> { process.setTxHash(hash); processRepository.save(process); });
    }

    public String recordExport(Export export) {
        return recordEntity("EXPORT", export.getId(),
                hash -> { export.setTxHash(hash); exportRepository.save(export); });
    }

    private String recordEntity(String entityType, Long entityId, java.util.function.Consumer<String> onConfirmed) {
        String idempotencyKey = entityType + "_" + entityId;
        Optional<BlockchainTransaction> existingTx = txRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            return existingTx.get().getTxHash();
        }

        if (isLive()) {
            String txId = broadcastAttestation(entityType, entityId);
            BlockchainTransaction tx = new BlockchainTransaction();
            tx.setEntityType(entityType);
            tx.setEntityId(entityId);
            tx.setTxHash(txId);
            tx.setIdempotencyKey(idempotencyKey);
            tx.setStatus("PENDING");
            txRepository.save(tx);
            return txId;
        }

        String txHash = generateMockTxHash();
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setEntityType(entityType);
        tx.setEntityId(entityId);
        tx.setTxHash(txHash);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setStatus("CONFIRMED");
        txRepository.save(tx);
        onConfirmed.accept(txHash);
        return txHash;
    }

    public boolean retryTransaction(Long txId) {
        BlockchainTransaction tx = txRepository.findById(txId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + txId));

        if ("CONFIRMED".equals(tx.getStatus())) {
            return true;
        }

        tx.setRetryCount(tx.getRetryCount() + 1);

        if (isLive()) {
            try {
                String newTxId = broadcastAttestation(tx.getEntityType(), tx.getEntityId());
                tx.setTxHash(newTxId);
                tx.setStatus("PENDING");
                txRepository.save(tx);
                return true;
            } catch (Exception e) {
                log.warn("Live retry failed for tx {}: {}", txId, e.getMessage());
                if (tx.getRetryCount() >= 3) {
                    tx.setStatus("FAILED");
                }
                txRepository.save(tx);
                return false;
            }
        }

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

    // ── LIVE BROADCAST ──────────────────────────────────────────────────────

    /**
     * Broadcasts an attestation anchor: a self-call clause carrying
     * blake2b256("BICAP:{type}:{id}:{timestamp}") as calldata. This writes an immutable,
     * publicly verifiable commitment to the entity on VeChainThor.
     */
    public String broadcastAttestation(String entityType, Long entityId) {
        byte[] data = Hashes.blake2b256(("BICAP:" + entityType + ":" + entityId + ":" + System.currentTimeMillis())
                .getBytes(StandardCharsets.UTF_8));
        String self = walletAddress();
        return broadcast(List.of(VeChainTxSigner.Clause.call(self, BigInteger.ZERO, data)), gasAttest, randomNonce());
    }

    private String broadcast(List<VeChainTxSigner.Clause> clauses, long gas, long nonce) {
        VeChainClient node = client();
        int chainTag = cachedChainTag(node);
        VeChainClient.BestBlock best = node.getBestBlock();
        // BlockRef = canonical uint64 of the reference block number (thor legacy tx).
        VeChainTxSigner.SignedTransaction signed = VeChainTxSigner.signType0(
                chainTag, best.number(), expiration, clauses,
                gasPriceCoef, gas, nonce, privateKeyBytes());
        return node.sendRawTransaction(signed.rawTx());
    }

    /** Genesis chain tag is constant per network — fetch once, then reuse. */
    private volatile Integer cachedChainTag;

    private int cachedChainTag(VeChainClient node) {
        Integer tag = cachedChainTag;
        if (tag == null) {
            synchronized (this) {
                tag = cachedChainTag;
                if (tag == null) {
                    tag = node.getChainTag();
                    cachedChainTag = tag;
                }
            }
        }
        return tag;
    }

    public String walletAddress() {
        return VeChainWallet.addressFromPrivateKey(privateKeyBytes());
    }

    private byte[] privateKeyBytes() {
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("blockchain.private-key is required when blockchain.mode=live");
        }
        return HexUtils.fromHex(privateKey);
    }

    private long randomNonce() {
        return Integer.toUnsignedLong(random.nextInt());
    }

    // ── MOCK HELPERS ────────────────────────────────────────────────────────

    private boolean executeBlockchainCall(String entityType, Long entityId) {
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
        return "0x" + java.util.UUID.randomUUID().toString().replace("-", "")
                + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String generateMockAddress() {
        String uuidPart1 = java.util.UUID.randomUUID().toString().replace("-", "");
        String uuidPart2 = java.util.UUID.randomUUID().toString().replace("-", "");
        return "0x" + (uuidPart1 + uuidPart2).substring(0, 40);
    }
}
