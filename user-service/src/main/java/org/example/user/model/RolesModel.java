package org.example.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Роль пользователя")
@Entity
@Table(name = "roles")
public class RolesModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(
            description = "ID роли",
            example = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long id;

    @NotBlank(message = "Role name is required")
    @Schema(
            description = "Название роли",
            example = "ADMIN"
    )
    private String name;

    public RolesModel() {
    }

    public RolesModel(Long id, String name) {
        this.id = id;
        this.name = name;
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
}