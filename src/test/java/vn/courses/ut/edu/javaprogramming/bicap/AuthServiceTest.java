package vn.courses.ut.edu.javaprogramming.bicap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.JwtTokenProvider;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AuthResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.LoginRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RegisterRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.UnauthorizedException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RoleRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.AuthService;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private Role retailerRole;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        retailerRole = Role.builder()
                .id(8L)
                .name("RETAILER")
                .permissions(Set.of())
                .build();
        registerRequest = new RegisterRequest(
                "Retailer User",
                "Retailer@Example.com",
                "0912345678",
                "Password@123",
                "Password@123"
        );
    }

    @Test
    void registerRetailerCreatesActiveRetailerWithHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("retailer@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0912345678")).thenReturn(false);
        when(roleRepository.findByName("RETAILER")).thenReturn(Optional.of(retailerRole));
        when(passwordEncoder.encode("Password@123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("access-token");

        AuthResponse response = authService.registerRetailer(registerRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("retailer@example.com", savedUser.getEmail());
        assertEquals("$2a$hashed", savedUser.getPassword());
        assertNotEquals(registerRequest.getPassword(), savedUser.getPassword());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertTrue(savedUser.getRoles().stream().anyMatch(role -> "RETAILER".equals(role.getName())));
        assertEquals("access-token", response.getAccessToken());
        assertTrue(response.getRoles().contains("RETAILER"));
    }

    @Test
    void registerRetailerRejectsMismatchedPasswordConfirmation() {
        registerRequest.setConfirmPassword("Different@123");

        assertThrows(BadRequestException.class, () -> authService.registerRetailer(registerRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRetailerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("retailer@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.registerRetailer(registerRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRetailerRejectsDuplicatePhone() {
        when(userRepository.existsByEmailIgnoreCase("retailer@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0912345678")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.registerRetailer(registerRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRetailerFailsWhenRetailerRoleIsMissing() {
        when(userRepository.existsByEmailIgnoreCase("retailer@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0912345678")).thenReturn(false);
        when(roleRepository.findByName("RETAILER")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.registerRetailer(registerRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginAcceptsEmail() {
        assertSuccessfulLogin("retailer@example.com");
    }

    @Test
    void loginAcceptsPhoneNumber() {
        assertSuccessfulLogin("0912345678");
    }

    @Test
    void loginRejectsInvalidCredentials() {
        LoginRequest request = new LoginRequest("retailer@example.com", "WrongPassword@123");
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    private void assertSuccessfulLogin(String identifier) {
        LoginRequest request = new LoginRequest(identifier, "Password@123");
        User retailer = User.builder()
                .id(10L)
                .email("retailer@example.com")
                .phone("0912345678")
                .fullName("Retailer User")
                .password("$2a$hashed")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(retailerRole))
                .build();
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                retailer,
                null,
                retailer.getAuthorities()
        );
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(jwtTokenProvider.generateToken(authenticated)).thenReturn("access-token");

        AuthResponse response = authService.login(request);

        ArgumentCaptor<Authentication> authenticationCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertEquals(identifier, authenticationCaptor.getValue().getPrincipal());
        assertEquals("access-token", response.getAccessToken());
        assertEquals("retailer@example.com", response.getEmail());
    }
}
