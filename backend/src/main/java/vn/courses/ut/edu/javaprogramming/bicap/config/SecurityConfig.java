package vn.courses.ut.edu.javaprogramming.bicap.config;

import jakarta.servlet.DispatcherType;

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
                    auth
                        // SSE (/api/notifications/stream) chạy ở chế độ async: sau khi controller
                        // trả SseEmitter, container dispatch lại request với DispatcherType.ASYNC.
                        // Ở lần dispatch này JwtAuthenticationFilter KHÔNG chạy
                        // (OncePerRequestFilter.shouldNotFilterAsyncDispatch() mặc định = true)
                        // nên SecurityContext rỗng → anyRequest().authenticated() ném
                        // AccessDeniedException dù response đã commit, sinh log
                        // "Unable to handle the Spring Security Exception because the response is
                        // already committed" và stream không đóng sạch.
                        // Request gốc đã qua xác thực + phân quyền ở lần dispatch REQUEST rồi,
                        // nên cho phép lần dispatch ASYNC đi tiếp (client không tự tạo được ASYNC dispatch).
                        //
                        // ERROR: khi async request kết thúc ở trạng thái lỗi (emitter ghi vào socket
                        // đã chết vì client reload/đóng tab), Tomcat dispatch tiếp trang lỗi /error
                        // với DispatcherType.ERROR — cũng không có SecurityContext. Nếu để
                        // anyRequest().authenticated() chặn thì chính lần dispatch trang lỗi lại ném
                        // AccessDeniedException: Access Denied + "response is already committed",
                        // lặp lại mỗi nhịp heartbeat 25s (xem SseAsyncDispatchSecurityTest).
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        // Trang lỗi mặc định của Spring Boot: chỉ trả JSON lỗi chuẩn hoá
                        // (server.error.include-message=never), không lộ chi tiết nội bộ.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/api/auth/**"
                        ).permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trace/**", "/api/marketplace/products/trace/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/notifications").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/service-packages/**").permitAll()

                        // Static shell of the unified web app served by Spring Boot itself
                        // (single-port setup): portal at "/", admin dashboard at "/admin".
                        // Only the HTML/asset shell is public — every data call still goes
                        // through authenticated /api/**.
                        .requestMatchers("/", "/index.html", "/favicon.ico", "/favicon.svg",
                                "/icons.svg", "/assets/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/trace/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin", "/admin/", "/admin/**").permitAll()

                        // Guest (anonymous) category lookup. The public catalogue and
                        // education endpoints live under "/api/public/**" (permitAll above)
                        // and only expose ACTIVE products / PUBLISHED articles.
                        // (C-3 fix: "/api/admin/products/**" used to be permitAll, which let
                        // anyone list unapproved products and, with a spoofed X-Actor-Email
                        // header, the full admin view.)
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                        // Liveness/health only — required for container healthchecks, no
                        // sensitive data exposed.
                        .requestMatchers("/actuator/health").permitAll()

                        .anyRequest().authenticated()
                );

        // M-6 mitigation: a strict Content-Security-Policy shrinks the XSS surface that
        // could otherwise exfiltrate the JWT stored in localStorage.
        // media-src allows the public educational videos (F5) to be streamed over https.
        http.headers(headers -> headers.contentSecurityPolicy(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data: https:; media-src 'self' https:; "
                        + "connect-src 'self'; frame-ancestors 'none'")
        );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
