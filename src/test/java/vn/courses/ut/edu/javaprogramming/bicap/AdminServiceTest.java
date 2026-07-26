package vn.courses.ut.edu.javaprogramming.bicap;

import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminCreateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.AdminUpdateRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Permission;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Role;
import vn.courses.ut.edu.javaprogramming.bicap.entity.User;
import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ConflictException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ForbiddenException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.PermissionRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.RoleRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.UserRepository;
import vn.courses.ut.edu.javaprogramming.bicap.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private AdminService adminService;

    private User superAdmin;
    private User normalAdmin;
    private Role superAdminRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        superAdminRole = Role.builder()
                .id(1L)
                .name("SUPER_ADMIN")
                .permissions(new HashSet<>())
                .build();

        adminRole = Role.builder()
                .id(2L)
                .name("ADMIN")
                .permissions(new HashSet<>())
                .build();

        superAdmin = User.builder()
                .id(1L)
                .email("super@bicap.com")
                .password("Secr3tPassword!")
                .fullName("Super Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(superAdminRole))
                .build();

        normalAdmin = User.builder()
                .id(2L)
                .email("admin@bicap.com")
                .password("Secr3tPassword!")
                .fullName("Normal Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build();
    }

    @Test
    void createAdmin_Successful() {
        // Arrange
        AdminCreateRequest request = AdminCreateRequest.builder()
                .email("new@bicap.com")
                .password("P@ssword123")
                .fullName("New Admin")
                .role("ADMIN")
                .permissions(Collections.emptyList())
                .build();

        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.existsByEmail("new@bicap.com")).thenReturn(false);
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AdminResponse response = adminService.createAdmin(request, "super@bicap.com");

        // Assert
        assertNotNull(response);
        assertEquals("new@bicap.com", response.getEmail());
        assertEquals("New Admin", response.getFullName());
        assertEquals(UserStatus.ACTIVE, response.getStatus()); // Defaults to ACTIVE (BR3)
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createAdmin_UnauthorizedActor_ThrowsForbiddenException() {
        // Arrange
        AdminCreateRequest request = AdminCreateRequest.builder()
                .email("new@bicap.com")
                .password("P@ssword123")
                .fullName("New Admin")
                .role("ADMIN")
                .build();

        when(userRepository.findByEmail("admin@bicap.com")).thenReturn(Optional.of(normalAdmin));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> 
                adminService.createAdmin(request, "admin@bicap.com")
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createAdmin_DuplicateEmail_ThrowsConflictException() {
        // Arrange
        AdminCreateRequest request = AdminCreateRequest.builder()
                .email("admin@bicap.com")
                .password("P@ssword123")
                .fullName("New Admin")
                .role("ADMIN")
                .build();

        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.existsByEmail("admin@bicap.com")).thenReturn(true);

        // Act & Assert
        assertThrows(ConflictException.class, () -> 
                adminService.createAdmin(request, "super@bicap.com")
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteAdmin_Successful_SoftDelete() {
        // Arrange
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(normalAdmin));

        // Act
        adminService.deleteAdmin(2L, "super@bicap.com");

        // Assert
        assertEquals(UserStatus.INACTIVE, normalAdmin.getStatus()); // Soft delete (BR2)
        verify(userRepository, times(1)).save(normalAdmin);
    }

    @Test
    void deleteAdmin_SelfDeletion_ThrowsBadRequestException() {
        // Arrange
        when(userRepository.findByEmail("super@bicap.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> 
                adminService.deleteAdmin(1L, "super@bicap.com")
        );
        verify(userRepository, never()).save(any(User.class));
    }
}
