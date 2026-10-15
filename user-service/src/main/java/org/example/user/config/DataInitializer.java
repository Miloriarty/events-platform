package org.example.user.config;

import org.example.user.model.RolesModel;
import org.example.user.model.UsersModel;
import org.example.user.repository.RolesRepository;
import org.example.user.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RolesRepository roles;
    private final UsersRepository users;
    private final PasswordEncoder encoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(RolesRepository roles, UsersRepository users, PasswordEncoder encoder,
                           @Value("${app.admin.username}") String adminUsername,
                           @Value("${app.admin.password}") String adminPassword) {
        this.roles = roles;
        this.users = users;
        this.encoder = encoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        for (String name : List.of("ADMIN", "MANAGER", "USER")) {
            if (roles.findByNameIgnoreCase(name).isEmpty()) {
                roles.save(new RolesModel(null, name));
            }
        }
        if (users.findFirstByUsernameIgnoreCase(adminUsername).isEmpty()) {
            Long adminRoleId = roles.findByNameIgnoreCase("ADMIN").get(0).getId();
            users.save(new UsersModel(null, adminUsername, encoder.encode(adminPassword),
                    "admin@mail.com", "Administrator", "+10000000000", adminRoleId)
            );
        }
    }
}