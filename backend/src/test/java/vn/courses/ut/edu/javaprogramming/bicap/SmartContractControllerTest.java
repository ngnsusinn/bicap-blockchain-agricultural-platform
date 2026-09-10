package vn.courses.ut.edu.javaprogramming.bicap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.courses.ut.edu.javaprogramming.bicap.controller.SmartContractController;
import vn.courses.ut.edu.javaprogramming.bicap.dto.DeployContractRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SmartContract;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.GlobalExceptionHandler;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.BlockchainService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SmartContractControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BlockchainService blockchainService;
    private UserRepository userRepository;
    private MockMvc mockMvc;

    private User superAdmin;
    private User normalUser;

    @BeforeEach
    void setUp() {
        blockchainService = mock(BlockchainService.class);
        userRepository = mock(UserRepository.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SmartContractController(blockchainService, userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        superAdmin = User.builder()
                .id(1L)
                .email("super@bicap.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().name("SUPER_ADMIN").build()))
                .build();

        normalUser = User.builder()
                .id(2L)
                .email("user@bicap.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.builder().name("RETAILER").build()))
                .build();
    }

    @Test
    void getContractsReturnsListForAdmin() throws Exception {
        SmartContract contract = new SmartContract();
        contract.setName("FarmingSeasonContract");
        contract.setVersion("1.0.0");

        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(blockchainService.getContracts()).thenReturn(List.of(contract));

        mockMvc.perform(get("/api/admin/contracts")
                        .header("X-Actor-Email", "super@bicap.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("FarmingSeasonContract"))
                .andExpect(jsonPath("$[0].version").value("1.0.0"));
    }

    @Test
    void getContractsRejectsNonAdmin() throws Exception {
        when(userRepository.findByEmail("user@bicap.com")).thenReturn(Optional.of(normalUser));

        mockMvc.perform(get("/api/admin/contracts")
                        .header("X-Actor-Email", "user@bicap.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deployContractSucceedsForSuperAdmin() throws Exception {
        DeployContractRequest request = new DeployContractRequest(
                "TraceabilityContract", "0xbytecode", "[]", "TESTNET", "1.0.0"
        );

        SmartContract sc = new SmartContract();
        sc.setName("TraceabilityContract");
        sc.setVersion("1.0.0");
        sc.setStatus("ACTIVE");

        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(blockchainService.deployContract(any(), any(), any(), any(), any())).thenReturn(sc);

        mockMvc.perform(post("/api/admin/contracts/deploy")
                        .header("X-Actor-Email", "super@bicap.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("TraceabilityContract"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deployContractFailsForNormalUser() throws Exception {
        DeployContractRequest request = new DeployContractRequest(
                "TraceabilityContract", "0xbytecode", "[]", "TESTNET", "1.0.0"
        );

        when(userRepository.findByEmail("user@bicap.com")).thenReturn(Optional.of(normalUser));

        mockMvc.perform(post("/api/admin/contracts/deploy")
                        .header("X-Actor-Email", "user@bicap.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
