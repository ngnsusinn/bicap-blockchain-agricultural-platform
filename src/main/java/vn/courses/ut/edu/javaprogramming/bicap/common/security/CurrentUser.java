package vn.courses.ut.edu.javaprogramming.bicap.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.UnauthorizedException;

import java.util.Set;

/**
 * Reads the authenticated user from the SecurityContext (set by {@link JwtAuthenticationFilter}).
 *
 * <p>Portal endpoints (farm/retail frontends) authenticate with the JWT only, so the acting
 * user is the {@code Principal} in the context — no client-supplied header is trusted for
 * identity here. Admin endpoints keep using {@link ActorAuthorizer} + {@code X-Actor-Email}
 * for consistency with the RBAC unit tests.
 */
public final class CurrentUser {

    private static final Set<String> ADMIN_VIEW_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "MODERATOR");

    private CurrentUser() {}

    /** Returns the authenticated user, throwing 401 when the request is unauthenticated. */
    public static User get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new UnauthorizedException("Authentication required");
    }

    /** True when the user holds an admin-view role (SUPER_ADMIN | ADMIN | MODERATOR). */
    public static boolean isAdminView(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> ADMIN_VIEW_ROLES.stream()
                        .anyMatch(r -> r.equalsIgnoreCase(role.getName())));
    }
}
