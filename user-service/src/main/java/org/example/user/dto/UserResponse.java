package org.example.user.dto;

import org.example.user.model.UsersModel;

import java.time.LocalDateTime;

public record UserResponse(
        Long id, String username, String email, String fullName,
        String phone, Long roleId, LocalDateTime createdAt) {

    public static UserResponse from(UsersModel u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(),
                u.getFullName(), u.getPhone(), u.getRoleId(), u.getCreatedAt()
        );
    }
}