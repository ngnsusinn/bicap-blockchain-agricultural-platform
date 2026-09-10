package vn.courses.ut.edu.javaprogramming.bicap;

import vn.courses.ut.edu.javaprogramming.bicap.common.security.ActorAuthorizer;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.UnauthorizedException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Single source of truth for actor role checks — all admin services delegate here,
 * so the role sets for view/write/super-admin capabilities are tested once.
 */
@ExtendWith(MockitoExtension.class)
class ActorAuthorizerTest {

    @Mock
    private UserRepository userRepository;

    private User superAdmin;
    private User admin;
    private User moderator;
    private User farmManager;

    @BeforeEach
    void setUp() {
        superAdmin = userWithRoles("SUPER_ADMIN");
        admin = userWithRoles("ADMIN");
        moderator = userWithRoles("MODERATOR");
        farmManager = userWithRoles("FARM_MANAGER");
    }

    private User userWithRoles(String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            roles.add(Role.builder().id((long) name.hashCode()).name(name).permissions(new HashSet<>()).build());
        }
        return User.builder()
                .id(1L).email("u@bicap.com").password("Secret@2026")
                .fullName("User").status(UserStatus.ACTIVE).roles(roles)
                .build();
    }

    @Test
    void requireSuperAdmin_shouldAllowOnlySuperAdmin() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.findByEmail("admin@bicap.com")).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() -> ActorAuthorizer.requireSuperAdmin(userRepository, "super@bicap.com"));
        assertThrows(ForbiddenException.class, () -> ActorAuthorizer.requireSuperAdmin(userRepository, "admin@bicap.com"));
    }

    @Test
    void requireAdminWrite_shouldAllowSuperAdminAndAdmin() {
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.findByEmail("admin@bicap.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("moderator@bicap.com")).thenReturn(Optional.of(moderator));

        assertDoesNotThrow(() -> ActorAuthorizer.requireAdminWrite(userRepository, "super@bicap.com"));
        assertDoesNotThrow(() -> ActorAuthorizer.requireAdminWrite(userRepository, "admin@bicap.com"));
        assertThrows(ForbiddenException.class, () -> ActorAuthorizer.requireAdminWrite(userRepository, "moderator@bicap.com"));
    }

    @Test
    void requireAdminView_shouldAllowAllAdminRolesButNotFarmManager() {
        when(userRepository.findByEmail("moderator@bicap.com")).thenReturn(Optional.of(moderator));
        when(userRepository.findByEmail("farm@bicap.com")).thenReturn(Optional.of(farmManager));

        assertDoesNotThrow(() -> ActorAuthorizer.requireAdminView(userRepository, "moderator@bicap.com"));
        assertThrows(ForbiddenException.class, () -> ActorAuthorizer.requireAdminView(userRepository, "farm@bicap.com"));
    }

    @Test
    void requireActor_missingOrUnknown_shouldThrowUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> ActorAuthorizer.requireSuperAdmin(userRepository, null));
        assertThrows(UnauthorizedException.class, () -> ActorAuthorizer.requireSuperAdmin(userRepository, "  "));
        when(userRepository.findByEmail("ghost@bicap.com")).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> ActorAuthorizer.requireSuperAdmin(userRepository, "ghost@bicap.com"));
    }
}
