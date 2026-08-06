package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating Farm Manager personal profile (BICAP-8).
 * Only editable fields: Avatar, Full Name, Phone Number, Address.
 * Read-only fields (Email, Password, Role, Status, Created Date) cannot be modified.
 */
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không vượt quá 255 ký tự")
    private String fullName;

    @Size(max = 15, message = "Số điện thoại không vượt quá 15 ký tự")
    private String phone;

    @Size(max = 500, message = "Địa chỉ không vượt quá 500 ký tự")
    private String address;

    @Size(max = 500, message = "URL ảnh đại diện không vượt quá 500 ký tự")
    private String avatarUrl;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String fullName, String phone, String address, String avatarUrl) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.avatarUrl = avatarUrl;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
