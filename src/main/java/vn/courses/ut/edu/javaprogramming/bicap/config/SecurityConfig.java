package vn.courses.ut.edu.javaprogramming.bicap.config;

import vn.courses.ut.edu.javaprogramming.bicap.common.security.CustomUserDetailsService;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.JwtAuthenticationFilter;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                    auth.requestMatchers(
                                "/api/auth/**"
                        ).permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trace/**", "/api/marketplace/products/trace/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/notifications").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/service-packages/**").permitAll()

                        // Static SPA shells served by Spring Boot itself (single-port setup):
                        // Farm Portal at "/", Admin Web at "/admin". Only the HTML/asset shells
                        // are public — every data call still goes through authenticated /api/**.
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/favicon.svg",
                                "/icons.svg", "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/trace/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin", "/admin/", "/admin/**").permitAll()

                        // Thêm quyền truy cập GET cho Guest (BICAP-70)
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/products/**").permitAll()

                        // Liveness/health only — required for container healthchecks, no
                        // sensitive data exposed.
                        .requestMatchers("/actuator/health").permitAll()

                        .anyRequest().authenticated()
                );

        // M-6 mitigation: a strict Content-Security-Policy shrinks the XSS surface that
        // could otherwise exfiltrate the JWT stored in localStorage.
        http.headers(headers -> headers.contentSecurityPolicy(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data: https:; connect-src 'self'; frame-ancestors 'none'")
        );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
