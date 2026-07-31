package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AuthResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.LoginRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RegisterRequest;
import vn.courses.ut.edu.javaprogramming.bicap.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ── Generic Endpoints ──
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerRetailer(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── Farm Manager Endpoints (BICAP-7) ──
    @PostMapping("/farm/register")
    public ResponseEntity<AuthResponse> registerFarm(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerFarmManager(request));
    }

    @PostMapping("/farm/login")
    public ResponseEntity<AuthResponse> loginFarm(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginFarmManager(request));
    }

    // ── Retailer Endpoints (BICAP-36) ──
    @PostMapping({"/retailer/register", "/retail/register"})
    public ResponseEntity<AuthResponse> registerRetailer(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerRetailer(request));
    }

    @PostMapping({"/retailer/login", "/retail/login"})
    public ResponseEntity<AuthResponse> loginRetailer(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginRetailer(request));
    }

    // ── Admin Endpoints ──
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request));
    }
}
