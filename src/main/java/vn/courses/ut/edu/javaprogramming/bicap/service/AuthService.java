package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class AuthService {

    private static final String RETAILER_ROLE = "RETAILER";
    private static final String FARM_MANAGER_ROLE = "FARM_MANAGER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final VerificationEmailService verificationEmailService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            VerificationEmailService verificationEmailService,
            LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.verificationEmailService = verificationEmailService;
        this.loginAttemptService = loginAttemptService;
    }

    public AuthResponse registerRetailer(RegisterRequest request) {
        User user = createUserWithRole(request, RETAILER_ROLE, UserStatus.ACTIVE);
        return issueStandardTokens(user);
    }

    public AuthResponse registerFarmManager(RegisterRequest request) {
        // H-7: the FARM role fallback was dead code (FARM is never seeded) and silently
        // hid misconfiguration. Resolve the intended role explicitly and fail fast.
        User user = createUserWithRole(request, FARM_MANAGER_ROLE, UserStatus.ACTIVE);
        return issueStandardTokens(user);
    }

    private User createUserWithRole(RegisterRequest request, String roleName, UserStatus status) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        // M-4: an existing account is NEVER mutated by a register call, and the email is
        // always reported as already registered (no "matching password" side channel that
        // both leaks account existence AND grants an extra role to the existing user).
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Phone number is already registered");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException(roleName + " role is not configured"));

        User user = User.builder()
                .email(email)
                .phone(phone)
                .fullName(request.getFullName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(status)
                .roles(Set.of(role))
                .build();

        return userRepository.save(user);
    }

    private AuthResponse issueStandardTokens(User savedUser) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser,
                null,
                savedUser.getAuthorities()
        );
        String accessToken = jwtTokenProvider.generateToken(authentication);

        return AuthResponse.fromUser(accessToken, savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        return authenticateAndBuildResponse(request);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginFarmManager(LoginRequest request) {
        User user = authenticateUser(request);
        boolean isFarmManager = user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("FARM_MANAGER") || r.getName().equalsIgnoreCase("FARM"));
        // M-3: a user who lacks the farm-manager role is NOT silently upgraded. Role
        // assignment is a privileged operation — this endpoint only admits farm managers.
        if (!isFarmManager) {
            throw new UnauthorizedException("Account is not authorized for the Farm portal");
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return AuthResponse.fromUser(accessToken, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginRetailer(LoginRequest request) {
        User user = authenticateUser(request);
        boolean isRetailer = user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("RETAILER"));
        // M-3: no silent role upgrade on login — a user either is a retailer or not.
        if (!isRetailer) {
            throw new UnauthorizedException("Account is not authorized for the Retailer portal");
        }
        String accessToken = jwtTokenProvider.generateRetailerAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        return AuthResponse.fromUser(accessToken, refreshToken, user);
    }

    public void verifyRetailerEmail(String token) {
        if (!jwtTokenProvider.isTokenType(token, "email_verification")) {
            throw new BadRequestException("Verification token is invalid or expired");
        }
        String email = jwtTokenProvider.getUsernameFromJWT(token);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Retailer account was not found"));
        boolean isRetailer = user.getRoles().stream()
                .anyMatch(role -> RETAILER_ROLE.equalsIgnoreCase(role.getName()));
        if (!isRetailer) {
            throw new BadRequestException("Verification token is not for a Retailer account");
        }
        if (user.getStatus() == UserStatus.ACTIVE) {
            return;
        }
        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Retailer account cannot be verified");
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public AuthResponse refreshRetailerToken(String refreshToken) {
        if (!jwtTokenProvider.isTokenType(refreshToken, "refresh")) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
        String email = jwtTokenProvider.getUsernameFromJWT(refreshToken);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Retailer account was not found"));
        boolean isRetailer = user.getRoles().stream()
                .anyMatch(role -> RETAILER_ROLE.equalsIgnoreCase(role.getName()));
        if (!isRetailer || user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not authorized for the Retailer portal");
        }
        String accessToken = jwtTokenProvider.generateRetailerAccessToken(user);
        String rotatedRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        return AuthResponse.fromUser(accessToken, rotatedRefreshToken, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginAdmin(LoginRequest request) {
        User user = authenticateUser(request);
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("ADMIN") || r.getName().equalsIgnoreCase("SUPER_ADMIN") || r.getName().equalsIgnoreCase("MODERATOR"));
        if (!isAdmin) {
            throw new UnauthorizedException("Account is not authorized for Admin portal");
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return AuthResponse.fromUser(accessToken, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginShippingMgr(LoginRequest request) {
        User user = authenticateUser(request);
        boolean isShippingMgr = user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("SHIPPING_MGR"));
        if (!isShippingMgr) {
            throw new UnauthorizedException("Account is not authorized for Shipping Manager portal");
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return AuthResponse.fromUser(accessToken, user);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginDriver(LoginRequest request) {
        User user = authenticateUser(request);
        boolean isDriver = user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("SHIP_DRIVER"));
        if (!isDriver) {
            throw new UnauthorizedException("Account is not authorized for Driver portal");
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return AuthResponse.fromUser(accessToken, user);
    }

    private User authenticateUser(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        User account = userRepository.findByEmailIgnoreCaseOrPhone(identifier, identifier).orElse(null);
        if (account != null) {
            if (account.getStatus() == UserStatus.SUSPENDED) {
                throw new UnauthorizedException("Account has been suspended");
            }
            if (account.getStatus() == UserStatus.PENDING_VERIFICATION) {
                throw new UnauthorizedException("Account email has not been verified");
            }
            if (account.getLockedUntil() != null
                    && account.getLockedUntil().isAfter(java.time.LocalDateTime.now())) {
                throw new UnauthorizedException("Account is temporarily locked for 30 minutes");
            }
            if (account.getLockedUntil() != null) {
                loginAttemptService.recordSuccess(account.getId());
            }
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );
            User authenticatedUser = (User) authentication.getPrincipal();
            loginAttemptService.recordSuccess(authenticatedUser.getId());
            return authenticatedUser;
        } catch (AuthenticationException exception) {
            if (account != null) {
                boolean lockTriggered = account.getFailedLoginAttempts() >= 4;
                loginAttemptService.recordFailure(account.getId());
                if (lockTriggered) {
                    throw new UnauthorizedException("Account is temporarily locked for 30 minutes");
                }
            }
            throw new UnauthorizedException("Email, phone number, or password is incorrect");
        }
    }

    private AuthResponse authenticateAndBuildResponse(LoginRequest request) {
        User user = authenticateUser(request);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return AuthResponse.fromUser(accessToken, user);
    }
}
