package org.example.user.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Пользователь системы")
@Entity
@Table(name = "users")
public class UsersModel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(
            description = "ID пользователя",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @NotBlank(message = "Username is required")
    @Schema(
            description = "Логин пользователя",
            example = "admin"
    )
    private String username;

    @NotBlank(message = "Password is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "Пароль пользователя", example = "password123",
            accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(
            description = "Email пользователя",
            example = "admin@mail.com"
    )
    private String email;

    @NotBlank(message = "Full name is required")
    @Schema(
            description = "Полное имя",
            example = "Ivan Ivanov"
    )
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Schema(
            description = "Телефон",
            example = "+79991234567"
    )
    private String phone;

    @NotNull(message = "Role ID is required")
    @Positive(message = "Role ID must be positive")
    @Schema(
            description = "ID роли пользователя",
            example = "1"
    )
    private Long roleId;

    @Schema(
            description = "Дата создания пользователя",
            example = "2026-05-31T12:00:00"
    )
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


    public UsersModel() {
    }

    public UsersModel(
            Long id,
            String username,
            String password,
            String email,
            String fullName,
            String phone,
            Long roleId
    ) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.roleId = roleId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}