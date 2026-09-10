package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import vn.courses.ut.edu.javaprogramming.bicap.config.SecretConfigValidator;
import vn.courses.ut.edu.javaprogramming.bicap.controller.BlockchainController;
import vn.courses.ut.edu.javaprogramming.bicap.entity.BlockchainTransaction;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.BlockchainTransactionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.BlockchainService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * BICAP-89 — security tests for the blockchain module:
 * read requires ADMIN_VIEW, retry requires ADMIN_WRITE, live mode requires a signer key.
 */
class BlockchainSecurityTest {

    private BlockchainService blockchainService;
    private BlockchainTransactionRepository txRepository;
    private UserRepository userRepository;
    private BlockchainController controller;

    @BeforeEach
    void setUp() {
        blockchainService = mock(BlockchainService.class);
        txRepository = mock(BlockchainTransactionRepository.class);
        userRepository = mock(UserRepository.class);
        controller = new BlockchainController(blockchainService, txRepository, userRepository);
    }

    private void actor(String email, String role) {
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(
                User.builder().id(1L).email(email).status(UserStatus.ACTIVE)
                        .roles(Set.of(Role.builder().name(role).build())).build()));
    }

    // ── Read: ADMIN_VIEW only ──────────────────────────────────────────────
    @Test
    void listTransactions_allowedForModerator() {
        actor("mod@bicap.com", "MODERATOR");
        when(txRepository.findAll()).thenReturn(List.of());
        ResponseEntity<List<BlockchainTransaction>> res =
                controller.getTransactions("mod@bicap.com");
        assertEquals(200, res.getStatusCode().value());
    }

    @Test
    void listTransactions_deniedForFarmManager() {
        actor("farm@bicap.com", "FARM_MANAGER");
        assertThrows(ForbiddenException.class, () -> controller.getTransactions("farm@bicap.com"));
        verify(txRepository, never()).findAll();
    }

    @Test
    void listTransactions_deniedForRetailer() {
        actor("retail@bicap.com", "RETAILER");
        assertThrows(ForbiddenException.class, () -> controller.getTransactions("retail@bicap.com"));
    }

    // ── Retry: ADMIN_WRITE only (MODERATOR is read-only) ───────────────────
    @Test
    void retry_deniedForModerator() {
        actor("mod@bicap.com", "MODERATOR");
        assertThrows(ForbiddenException.class, () -> controller.retryTransaction("mod@bicap.com", 1L));
        verify(blockchainService, never()).retryTransaction(anyLong());
    }

    @Test
    void retry_allowedForAdmin() {
        actor("admin@bicap.com", "ADMIN");
        when(blockchainService.retryTransaction(5L)).thenReturn(true);
        ResponseEntity<Map<String, Object>> res = controller.retryTransaction("admin@bicap.com", 5L);
        assertEquals(200, res.getStatusCode().value());
        verify(blockchainService).retryTransaction(5L);
    }

    @Test
    void retry_deniedForUnknownActor() {
        when(userRepository.findByEmail("ghost@bicap.com")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> controller.retryTransaction("ghost@bicap.com", 5L));
        verify(blockchainService, never()).retryTransaction(anyLong());
    }

    // ── Live-mode key requirement (SecretConfigValidator) ──────────────────
    @Test
    void validator_liveModeWithoutPrivateKey_failsFast() {
        SecretConfigValidator validator = new SecretConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "dGVzdC1qd3Qtc2VjcmV0LWtleS1hdC1sZWFzdC0zMi1ieXRlcw==");
        ReflectionTestUtils.setField(validator, "sepayApiKey", "real-key");
        ReflectionTestUtils.setField(validator, "blockchainMode", "live");
        ReflectionTestUtils.setField(validator, "blockchainPrivateKey", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(validator, "validate"));
        assertTrue(ex.getMessage().contains("BLOCKCHAIN_PRIVATE_KEY"));
    }

    @Test
    void validator_liveModeWithKey_passes() {
        SecretConfigValidator validator = new SecretConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "dGVzdC1qd3Qtc2VjcmV0LWtleS1hdC1sZWFzdC0zMi1ieXRlcw==");
        ReflectionTestUtils.setField(validator, "sepayApiKey", "real-key");
        ReflectionTestUtils.setField(validator, "blockchainMode", "live");
        ReflectionTestUtils.setField(validator, "blockchainPrivateKey", "4646464646464646464646464646464646464646464646464646464646464646");

        ReflectionTestUtils.invokeMethod(validator, "validate");
    }

    @Test
    void validator_mockModeWithoutKey_passes() {
        SecretConfigValidator validator = new SecretConfigValidator();
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "dGVzdC1qd3Qtc2VjcmV0LWtleS1hdC1sZWFzdC0zMi1ieXRlcw==");
        ReflectionTestUtils.setField(validator, "sepayApiKey", "real-key");
        ReflectionTestUtils.setField(validator, "blockchainMode", "mock");
        ReflectionTestUtils.setField(validator, "blockchainPrivateKey", "");

        ReflectionTestUtils.invokeMethod(validator, "validate");
    }
}
