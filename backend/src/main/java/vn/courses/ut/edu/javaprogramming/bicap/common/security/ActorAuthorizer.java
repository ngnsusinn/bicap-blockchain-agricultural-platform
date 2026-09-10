package vn.courses.ut.edu.javaprogramming.bicap.common.security;

import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.UnauthorizedException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;

import java.util.Set;

/**
 * Single source of truth for actor authorization checks based on the X-Actor-Email header.
 *
 * <p>Every admin service (AdminService, FarmApprovalService...) must go through these
 * helpers instead of re-implementing "find user by email → check roles" so that the
 * role sets stay in lockstep when the RBAC model changes.
 *
 * <p>Role sets per capability:
 * <ul>
 *   <li>{@link #requireSuperAdmin} — SUPER_ADMIN only (account CRUD)</li>
 *   <li>{@link #requireAdminWrite} — SUPER_ADMIN | ADMIN (approve/reject, manage farms)</li>
 *   <li>{@link #requireAdminView} — SUPER_ADMIN | ADMIN | MODERATOR (read-only listings)</li>
 * </ul>
 */
public final class ActorAuthorizer {

    private static final Set<String> SUPER_ADMIN_ROLES = Set.of("SUPER_ADMIN");
    private static final Set<String> ADMIN_WRITE_ROLES = Set.of("SUPER_ADMIN", "ADMIN");
    private static final Set<String> ADMIN_VIEW_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "MODERATOR");

    private ActorAuthorizer() {}

    /** Loads the acting user from the X-Actor-Email header value. */
    public static User requireActor(UserRepository userRepository, String actorEmail) {
        if (actorEmail == null || actorEmail.trim().isEmpty()) {
            throw new UnauthorizedException("Actor email header is missing");
        }
        return userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new UnauthorizedException("Actor not found"));
    }

    /** Throws ForbiddenException unless the user holds at least one of the given roles. */
    public static void requireRoles(User actor, Set<String> allowedRoles) {
        boolean allowed = actor.getRoles().stream()
                .anyMatch(role -> allowedRoles.stream()
                        .anyMatch(allowedRole -> allowedRole.equalsIgnoreCase(role.getName())));
        if (!allowed) {
            throw new ForbiddenException("Insufficient permissions for this operation");
        }
    }

    public static User requireSuperAdmin(UserRepository userRepository, String actorEmail) {
        User actor = requireActor(userRepository, actorEmail);
        requireRoles(actor, SUPER_ADMIN_ROLES);
        return actor;
    }

    public static User requireAdminWrite(UserRepository userRepository, String actorEmail) {
        User actor = requireActor(userRepository, actorEmail);
        requireRoles(actor, ADMIN_WRITE_ROLES);
        return actor;
    }

    public static User requireAdminView(UserRepository userRepository, String actorEmail) {
        User actor = requireActor(userRepository, actorEmail);
        requireRoles(actor, ADMIN_VIEW_ROLES);
        return actor;
    }
}
