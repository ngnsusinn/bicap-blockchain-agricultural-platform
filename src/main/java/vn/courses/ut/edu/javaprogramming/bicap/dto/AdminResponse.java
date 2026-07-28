package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.UserStatus;
import java.util.Set;
import java.util.HashSet;

public class AdminResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private UserStatus status;
    private String avatarUrl;
    private Set<RoleResponse> roles;

    public AdminResponse() {}

    public AdminResponse(Long id, String email, String fullName, String phone, UserStatus status, String avatarUrl, Set<RoleResponse> roles) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.status = status;
        this.avatarUrl = avatarUrl;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Set<RoleResponse> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleResponse> roles) {
        this.roles = roles;
    }

    public static class RoleResponse {
        private Long id;
        private String name;
        private String description;
        private Set<PermissionResponse> permissions;

        public RoleResponse() {}

        public RoleResponse(Long id, String name, String description, Set<PermissionResponse> permissions) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.permissions = permissions;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Set<PermissionResponse> getPermissions() {
            return permissions;
        }

        public void setPermissions(Set<PermissionResponse> permissions) {
            this.permissions = permissions;
        }

        public static RoleResponseBuilder builder() {
            return new RoleResponseBuilder();
        }

        public static class RoleResponseBuilder {
            private Long id;
            private String name;
            private String description;
            private Set<PermissionResponse> permissions;

            RoleResponseBuilder() {}

            public RoleResponseBuilder id(Long id) {
                this.id = id;
                return this;
            }

            public RoleResponseBuilder name(String name) {
                this.name = name;
                return this;
            }

            public RoleResponseBuilder description(String description) {
                this.description = description;
                return this;
            }

            public RoleResponseBuilder permissions(Set<PermissionResponse> permissions) {
                this.permissions = permissions;
                return this;
            }

            public RoleResponse build() {
                return new RoleResponse(id, name, description, permissions);
            }
        }
    }

    public static class PermissionResponse {
        private Long id;
        private String code;
        private String description;

        public PermissionResponse() {}

        public PermissionResponse(Long id, String code, String description) {
            this.id = id;
            this.code = code;
            this.description = description;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public static PermissionResponseBuilder builder() {
            return new PermissionResponseBuilder();
        }

        public static class PermissionResponseBuilder {
            private Long id;
            private String code;
            private String description;

            PermissionResponseBuilder() {}

            public PermissionResponseBuilder id(Long id) {
                this.id = id;
                return this;
            }

            public PermissionResponseBuilder code(String code) {
                this.code = code;
                return this;
            }

            public PermissionResponseBuilder description(String description) {
                this.description = description;
                return this;
            }

            public PermissionResponse build() {
                return new PermissionResponse(id, code, description);
            }
        }
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

    public static AdminResponseBuilder builder() {
        return new AdminResponseBuilder();
    }

    public static class AdminResponseBuilder {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private UserStatus status;
        private String avatarUrl;
        private Set<RoleResponse> roles;

        AdminResponseBuilder() {}

        public AdminResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AdminResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public AdminResponseBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public AdminResponseBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public AdminResponseBuilder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public AdminResponseBuilder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public AdminResponseBuilder roles(Set<RoleResponse> roles) {
            this.roles = roles;
            return this;
        }

        public AdminResponse build() {
            return new AdminResponse(id, email, fullName, phone, status, avatarUrl, roles);
        }
    }
}
