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
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Password confirmation does not match");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ConflictException("Phone number is already registered");
        }

        Role retailerRole = roleRepository.findByName(RETAILER_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("RETAILER role is not configured"));

        User retailer = User.builder()
                .email(email)
                .phone(phone)
                .fullName(request.getFullName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(retailerRole))
                .build();

        User savedRetailer = userRepository.save(retailer);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedRetailer,
                null,
                savedRetailer.getAuthorities()
        );
        String accessToken = jwtTokenProvider.generateToken(authentication);

        return AuthResponse.fromUser(accessToken, savedRetailer);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
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
