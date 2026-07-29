package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private String description;

    public Permission() {}

    public Permission(Long id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public static PermissionBuilder builder() {
        return new PermissionBuilder();
    }

    public static class PermissionBuilder {
        private Long id;
        private String code;
        private String description;

        public PermissionBuilder id(Long id) { this.id = id; return this; }
        public PermissionBuilder code(String code) { this.code = code; return this; }
        public PermissionBuilder description(String description) { this.description = description; return this; }

        public Permission build() {
            return new Permission(id, code, description);
        }
    }
}
