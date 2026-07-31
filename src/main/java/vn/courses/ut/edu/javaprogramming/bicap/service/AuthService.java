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
    private static final String FARM_ROLE = "FARM";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse registerRetailer(RegisterRequest request) {
        return registerUserWithRole(request, RETAILER_ROLE);
    }

    public AuthResponse registerFarmManager(RegisterRequest request) {
        try {
            return registerUserWithRole(request, FARM_MANAGER_ROLE);
        } catch (ResourceNotFoundException e) {
            return registerUserWithRole(request, FARM_ROLE);
        }
    }

    private AuthResponse registerUserWithRole(RegisterRequest request, String roleName) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException(roleName + " role is not configured"));

        var existingUserOpt = userRepository.findByEmailIgnoreCase(email);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
                existingUser.getRoles().add(role);
                User savedUser = userRepository.save(existingUser);
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        savedUser,
                        null,
                        savedUser.getAuthorities()
                );
                String accessToken = jwtTokenProvider.generateToken(authentication);
                return AuthResponse.fromUser(accessToken, savedUser);
            } else {
                throw new ConflictException("Email is already registered with a different password");
            }
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Phone number is already registered");
        }

        User user = User.builder()
                .email(email)
                .phone(phone)
                .fullName(request.getFullName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(role))
                .build();

        User savedUser = userRepository.save(user);
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

    @Transactional
    public AuthResponse loginFarmManager(LoginRequest request) {
        User user = authenticateUser(request);
        boolean isFarmManager = user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("FARM_MANAGER") || r.getName().equalsIgnoreCase("FARM"));
        if (!isFarmManager) {
            Role role = roleRepository.findByName(FARM_MANAGER_ROLE)
                    .orElseGet(() -> roleRepository.findByName(FARM_ROLE)
                            .orElseThrow(() -> new ResourceNotFoundException("FARM_MANAGER role is not configured")));
            user.getRoles().add(role);
            user = userRepository.save(user);
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return AuthResponse.fromUser(accessToken, user);
    }

    @Transactional
    public AuthResponse loginRetailer(LoginRequest request) {
        User user = authenticateUser(request);
        boolean isRetailer = user.getRoles().stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase("RETAILER"));
        if (!isRetailer) {
            Role role = roleRepository.findByName(RETAILER_ROLE)
                    .orElseThrow(() -> new ResourceNotFoundException("RETAILER role is not configured"));
            user.getRoles().add(role);
            user = userRepository.save(user);
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        String accessToken = jwtTokenProvider.generateToken(authentication);
        return AuthResponse.fromUser(accessToken, user);
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

    private User authenticateUser(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );
            return (User) authentication.getPrincipal();
        } catch (AuthenticationException exception) {
            throw new UnauthorizedException("Email, phone number, or password is incorrect");
        }
    }

    private AuthResponse authenticateAndBuildResponse(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );
            User user = (User) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.generateToken(authentication);
            return AuthResponse.fromUser(accessToken, user);
        } catch (AuthenticationException exception) {
            throw new UnauthorizedException("Email, phone number, or password is incorrect");
        }
    }
}
