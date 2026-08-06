package vn.courses.ut.edu.javaprogramming.bicap.service;

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
    }
}
