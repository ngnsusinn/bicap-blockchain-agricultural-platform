package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.User;

public record RetailerProfileResponse(
        Long userId,
        String email,
        String fullName,
        String phone,
        String address,
        String avatarUrl
) {
    public static RetailerProfileResponse from(User user) {
        return new RetailerProfileResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getAddress(), user.getAvatarUrl()
        );
    }
}
