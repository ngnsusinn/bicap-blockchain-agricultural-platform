package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.dto.*;
import vn.courses.ut.edu.javaprogramming.bicap.entity.*;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.*;
import vn.courses.ut.edu.javaprogramming.bicap.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonExportServiceTest {
    @Mock SeasonExportRepository exports;
    @Mock FarmRepository farms;
    @Mock FarmingSeasonExportGateway seasons;
    @Mock ExportBlockchainGateway blockchain;
    @Mock QrCodeService qrCodes;
    SeasonExportService service;

    @BeforeEach void setUp() {
        service = new SeasonExportService(exports, farms, seasons, blockchain, qrCodes);
        Role role = Role.builder().name("FARM_MANAGER").permissions(Set.of()).build();
        User user = User.builder().id(7L).email("farm@bicap.vn").status(UserStatus.ACTIVE).roles(Set.of(role)).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(farms.findById(2L)).thenReturn(Optional.of(Farm.builder().id(2L).userId(7L).name("Farm").address("A").area(1d).build()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void create_generatesOneQrAfterBlockchainReceipt() {
        when(exports.findByIdempotencyKey("request-1")).thenReturn(Optional.empty());
        when(seasons.requireHarvested(2L, 9L)).thenReturn(new FarmingSeasonExportGateway.SeasonSnapshot(9L,"Rice",new BigDecimal("100"),"kg"));
        when(exports.sumCommittedQuantity(9L, ExportStatus.BLOCKCHAIN_FAILED)).thenReturn(new BigDecimal("40"));
        when(exports.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(exports.save(any())).thenAnswer(i -> i.getArgument(0));
        String txHash = "0x" + "a".repeat(64);
        when(blockchain.recordExport(any())).thenReturn(txHash);
        when(qrCodes.pngDataUri(anyString())).thenReturn("data:image/png;base64,abc");

        var result = service.create(2L, 9L, request("60"), "request-1");

        assertEquals("READY", result.status()); assertEquals(txHash, result.transactionHash());
        verify(blockchain).recordExport(any()); verify(qrCodes).pngDataUri(anyString());
    }

    @Test void create_rejectsQuantityAboveRemainingHarvest() {
        when(exports.findByIdempotencyKey("request-2")).thenReturn(Optional.empty());
        when(seasons.requireHarvested(2L, 9L)).thenReturn(new FarmingSeasonExportGateway.SeasonSnapshot(9L,"Rice",new BigDecimal("100"),"kg"));
        when(exports.sumCommittedQuantity(9L, ExportStatus.BLOCKCHAIN_FAILED)).thenReturn(new BigDecimal("80"));
        assertThrows(BadRequestException.class, () -> service.create(2L, 9L, request("21"), "request-2"));
        verifyNoInteractions(blockchain, qrCodes);
    }

    @Test void create_marksQrFailureWithoutMisreportingBlockchainFailure() {
        when(exports.findByIdempotencyKey("request-3")).thenReturn(Optional.empty());
        when(seasons.requireHarvested(2L, 9L)).thenReturn(new FarmingSeasonExportGateway.SeasonSnapshot(9L,"Rice",new BigDecimal("100"),"kg"));
        when(exports.sumCommittedQuantity(9L, ExportStatus.BLOCKCHAIN_FAILED)).thenReturn(BigDecimal.ZERO);
        when(exports.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0)); when(exports.save(any())).thenAnswer(i -> i.getArgument(0));
        when(blockchain.recordExport(any())).thenReturn("0x" + "b".repeat(64));
        when(qrCodes.pngDataUri(anyString())).thenThrow(new IllegalStateException("QR error"));
        assertEquals("QR_FAILED", service.create(2L, 9L, request("10"), "request-3").status());
    }

    private SeasonExportRequest request(String quantity) {
        return new SeasonExportRequest(new BigDecimal(quantity), "kg", LocalDate.now(), "Kho A");
    }
}
