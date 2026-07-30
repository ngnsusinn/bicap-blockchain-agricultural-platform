package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(regexp = "(?i)^[^\\s@]+@gmail\\.com$", message = "Email must be a Gmail address ending with @gmail.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^()+=.\\-])[A-Za-z\\d@$!%*?&_#^()+=.\\-]{8,}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    private String password;

    @Pattern(regexp = "^(0\\d{9})?$", message = "Phone number must be a valid 10-digit Vietnamese number starting with 0")
    private String phone;
}
