package vn.courses.ut.edu.javaprogramming.bicap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.CustomUserDetailsService;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.JwtAuthenticationFilter;
import vn.courses.ut.edu.javaprogramming.bicap.common.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JWT filter security: the X-Actor-Email header must NEVER authenticate on its own,
 * and when a JWT is present the header (if supplied) must match the token's user.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    private static final String JWT = "valid-jwt-token";
    private static final String USER_EMAIL = "admin@bicap.com";

    private final UserDetails adminDetails = User.withUsername(USER_EMAIL)
            .password("x").authorities("ROLE_ADMIN").build();

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noJwt_withActorHeader_shouldNotAuthenticate() throws Exception {
        // X-Actor-Email alone must never authenticate — no JWT means no identity.
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void validJwt_withMatchingHeader_shouldAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + JWT);
        when(tokenProvider.validateToken(JWT)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(JWT)).thenReturn(USER_EMAIL);
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(adminDetails);
        when(request.getHeader("X-Actor-Email")).thenReturn(USER_EMAIL);

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(USER_EMAIL, ((UserDetails) auth.getPrincipal()).getUsername());
    }

    @Test
    void validJwt_withMismatchedHeader_shouldNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + JWT);
        when(tokenProvider.validateToken(JWT)).thenReturn(true);
        when(tokenProvider.getUsernameFromJWT(JWT)).thenReturn(USER_EMAIL);
        when(userDetailsService.loadUserByUsername(USER_EMAIL)).thenReturn(adminDetails);
        when(request.getHeader("X-Actor-Email")).thenReturn("superadmin@bicap.com");

        filter.doFilter(request, response, filterChain);

        // Impersonation attempt — no identity is set, the request will be rejected by
        // SecurityConfig's authenticated() rule.
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidJwt_shouldNotAuthenticate() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + JWT);
        when(tokenProvider.validateToken(JWT)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
