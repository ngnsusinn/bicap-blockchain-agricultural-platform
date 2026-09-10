package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
import vn.courses.ut.edu.javaprogramming.bicap.service.BlockchainService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class BlockchainServiceTest {

    @Mock
    private BlockchainTransactionRepository txRepository;

    @Mock
    private SmartContractRepository contractRepository;

    @Mock
    private FarmingSeasonRepository seasonRepository;

    @Mock
    private FarmingProcessRepository processRepository;

    @Mock
    private ExportRepository exportRepository;

    @InjectMocks
    private BlockchainService blockchainService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(blockchainService, "mode", "mock");
    }

    @Test
    void getContractsReturnsAll() {
        SmartContract sc = new SmartContract();
        sc.setName("Traceability");
        when(contractRepository.findAll()).thenReturn(List.of(sc));

        List<SmartContract> result = blockchainService.getContracts();
        assertEquals(1, result.size());
        assertEquals("Traceability", result.get(0).getName());
    }

    @Test
    void deployContractCreatesRecord() {
        SmartContract sc = new SmartContract();
        sc.setId(1L);
        sc.setName("Traceability");
        when(contractRepository.save(any(SmartContract.class))).thenReturn(sc);

        SmartContract result = blockchainService.deployContract(
                "Traceability", "0xbytecode", "[]", "TESTNET", "1.0.0"
        );

        assertNotNull(result);
        assertEquals("Traceability", result.getName());
        verify(contractRepository, times(2)).save(any(SmartContract.class));
        verify(txRepository, times(1)).save(any(BlockchainTransaction.class));
    }

    @Test
    void recordSeasonSavesTransaction() {
        FarmingSeason season = new FarmingSeason();
        season.setId(10L);

        when(txRepository.findByIdempotencyKey("SEASON_10")).thenReturn(Optional.empty());

        String txHash = blockchainService.recordSeason(season);
        assertNotNull(txHash);
        assertTrue(txHash.startsWith("0x"));

        verify(txRepository, times(1)).save(any(BlockchainTransaction.class));
        verify(seasonRepository, times(1)).save(season);
    }

    @Test
    void recordProcessSavesTransaction() {
        FarmingProcess process = new FarmingProcess();
        process.setId(20L);

        when(txRepository.findByIdempotencyKey("PROCESS_20")).thenReturn(Optional.empty());

        String txHash = blockchainService.recordProcess(process);
        assertNotNull(txHash);

        verify(txRepository, times(1)).save(any(BlockchainTransaction.class));
        verify(processRepository, times(1)).save(process);
    }

    @Test
    void recordExportSavesTransaction() {
        Export export = new Export();
        export.setId(30L);

        when(txRepository.findByIdempotencyKey("EXPORT_30")).thenReturn(Optional.empty());

        String txHash = blockchainService.recordExport(export);
        assertNotNull(txHash);

        verify(txRepository, times(1)).save(any(BlockchainTransaction.class));
        verify(exportRepository, times(1)).save(export);
    }

    @Test
    void retryTransactionReturnsSuccessForConfirmed() {
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setStatus("CONFIRMED");
        when(txRepository.findById(1L)).thenReturn(Optional.of(tx));

        boolean result = blockchainService.retryTransaction(1L);
        assertTrue(result);
    }

    @Test
    void retryTransactionInvokesCallOnFailure() {
        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setId(1L);
        tx.setStatus("FAILED");
        tx.setEntityType("SEASON");
        tx.setEntityId(10L);
        tx.setTxHash("0xmockhash");
        tx.setRetryCount(0);

        when(txRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(new FarmingSeason()));

        boolean result = blockchainService.retryTransaction(1L);
        assertTrue(result);
        assertEquals("CONFIRMED", tx.getStatus());
        assertEquals(1, tx.getRetryCount());
    }
}
