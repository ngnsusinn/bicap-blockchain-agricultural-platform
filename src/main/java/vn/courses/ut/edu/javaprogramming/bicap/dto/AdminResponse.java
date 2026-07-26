package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import lombok.*;
import java.util.Set;
import java.util.HashSet;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private UserStatus status;
    private String avatarUrl;
    private Set<RoleResponse> roles;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoleResponse {
        private Long id;
        private String name;
        private String description;
        private Set<PermissionResponse> permissions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionResponse {
        private Long id;
        private String code;
        private String description;
    }

    public static AdminResponse fromUser(vn.courses.ut.edu.javaprogramming.bicap.entity.User user) {
        if (user == null) {
            return null;
        }
        
        Set<RoleResponse> roleResponses = new HashSet<>();
        if (user.getRoles() != null) {
            for (vn.courses.ut.edu.javaprogramming.bicap.entity.Role role : user.getRoles()) {
                Set<PermissionResponse> permResponses = new HashSet<>();
                if (role.getPermissions() != null) {
                    for (vn.courses.ut.edu.javaprogramming.bicap.entity.Permission perm : role.getPermissions()) {
                        permResponses.add(PermissionResponse.builder()
                                .id(perm.getId())
                                .code(perm.getCode())
                                .description(perm.getDescription())
                                .build());
                    }
                }
                roleResponses.add(RoleResponse.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .permissions(permResponses)
                        .build());
            }
        }

        return AdminResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleResponses)
                .build();
    }
}
