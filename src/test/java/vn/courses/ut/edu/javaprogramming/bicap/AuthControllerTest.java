package vn.courses.ut.edu.javaprogramming.bicap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.courses.ut.edu.javaprogramming.bicap.controller.AuthController;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AuthResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.LoginRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RegisterRequest;
import vn.courses.ut.edu.javaprogramming.bicap.exception.GlobalExceptionHandler;
import vn.courses.ut.edu.javaprogramming.bicap.service.AuthService;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Retailer User",
                "retailer@example.com",
                "0912345678",
                "Password@123",
                "Password@123"
        );
        when(authService.registerRetailer(any(RegisterRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("retailer@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("RETAILER"));

        verify(authService).registerRetailer(any(RegisterRequest.class));
    }

    @Test
    void registerRejectsInvalidInput() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "R",
                "invalid-email",
                "123",
                "weak",
                "weak"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void loginReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest("0912345678", "Password@123");
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.phone").value("0912345678"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void loginRejectsBlankIdentifier() throws Exception {
        LoginRequest request = new LoginRequest("", "Password@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void farmRegisterReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Farm Owner",
                "farm@example.com",
                "0987654321",
                "Password@123",
                "Password@123"
        );
        when(authService.registerFarmManager(any(RegisterRequest.class))).thenReturn(farmAuthResponse());

        mockMvc.perform(post("/api/auth/farm/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("farm@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("FARM_MANAGER"));

        verify(authService).registerFarmManager(any(RegisterRequest.class));
    }

    @Test
    void farmLoginReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest("0987654321", "Password@123");
        when(authService.loginFarmManager(any(LoginRequest.class))).thenReturn(farmAuthResponse());

        mockMvc.perform(post("/api/auth/farm/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("farm-access-token"));

        verify(authService).loginFarmManager(any(LoginRequest.class));
    }

    @Test
    void retailerLoginReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest("0912345678", "Password@123");
        when(authService.loginRetailer(any(LoginRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/retailer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(authService).loginRetailer(any(LoginRequest.class));
    }

    @Test
    void retailerRegisterReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Retailer User",
                "retailer@example.com",
                "0912345678",
                "Password@123",
                "Password@123"
        );
        when(authService.registerRetailer(any(RegisterRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/retailer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.roles[0]").value("RETAILER"));

        verify(authService).registerRetailer(any(RegisterRequest.class));
    }

    @Test
    void retailerEmailVerificationReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/retailer/verify-email")
                        .param("token", "verification-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        verify(authService).verifyRetailerEmail("verification-token");
    }

    @Test
    void retailerRefreshReturnsRotatedTokens() throws Exception {
        AuthResponse refreshed = new AuthResponse(
                "new-access-token", "new-refresh-token", "Bearer", 10L,
                "retailer@example.com", "0912345678", "Retailer User",
                Set.of("RETAILER"), false
        );
        when(authService.refreshRetailerToken("refresh-token")).thenReturn(refreshed);

        mockMvc.perform(post("/api/auth/retailer/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        verify(authService).refreshRetailerToken("refresh-token");
    }

    @Test
    void adminLoginReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest("admin@bicap.com", "Password@123");
        AuthResponse adminResponse = new AuthResponse("admin-token", "Bearer", 1L, "admin@bicap.com", "0900000000", "System Admin", Set.of("ADMIN"));
        when(authService.loginAdmin(any(LoginRequest.class))).thenReturn(adminResponse);

        mockMvc.perform(post("/api/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("admin-token"));

        verify(authService).loginAdmin(any(LoginRequest.class));
    }

    private AuthResponse authResponse() {
        return new AuthResponse(
                "access-token",
                "Bearer",
                10L,
                "retailer@example.com",
                "0912345678",
                "Retailer User",
                Set.of("RETAILER")
        );
    }

    private AuthResponse farmAuthResponse() {
        return new AuthResponse(
                "farm-access-token",
                "Bearer",
                11L,
                "farm@example.com",
                "0987654321",
                "Farm Owner",
                Set.of("FARM_MANAGER")
        );
    }
}
