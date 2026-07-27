package vn.courses.ut.edu.javaprogramming.bicap.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RoleRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    private static final String FARM_ROLE = "FARM_USER";

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already in use: " + request.getEmail());
        }

        // Ensure FARM_USER role exists or create a simple role entry
        Role farmRole = roleRepository.findByName(FARM_ROLE)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(FARM_ROLE)
                        .description("Farm user role")
                        .permissions(null)
                        .build()));

        Set<Role> roles = new HashSet<>();
        roles.add(farmRole);

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status(UserStatus.ACTIVE)
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        // Optionally auto-login and return token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String jwt = tokenProvider.generateToken(authentication);
        return AuthResponse.fromUser(saved, jwt);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String jwt = tokenProvider.generateToken(authentication);

        // load user to include details in response
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return AuthResponse.fromUser(user, jwt);
    }
}
