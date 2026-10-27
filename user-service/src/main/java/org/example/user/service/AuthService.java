package org.example.user.service;

import org.example.user.dto.LoginRequest;
import org.example.user.dto.LoginResponse;
import org.example.user.dto.RegisterRequest;
import org.example.user.dto.UserResponse;
import org.example.user.model.RolesModel;
import org.example.user.model.UsersModel;
import org.example.user.repository.RolesRepository;
import org.example.user.repository.UsersRepository;
import org.example.user.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsersRepository users;
    private final RolesRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsersRepository users, RolesRepository roles,
                       PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public LoginResponse login(LoginRequest req) {
        // одинаковая ошибка и для «нет пользователя», и для «неверный пароль»
        UsersModel user = users.findFirstByUsernameIgnoreCase(req.username())
                .filter(u -> encoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> {
                    log.warn("Failed login attempt for username '{}'", req.username());
                    return new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid username or password");
                });

        String role = roles.findById(user.getRoleId())
                .map(RolesModel::getName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "User role is not configured"));

        String token = jwt.generateToken(user.getId(), user.getUsername(), role);
        log.info("User '{}' (id={}, role={}) logged in", user.getUsername(), user.getId(), role);
        return new LoginResponse(token, "Bearer", jwt.getExpirationSeconds());
    }

    public UserResponse register(RegisterRequest req) {
        if (users.existsByUsernameIgnoreCase(req.username())) {
            log.warn("Registration rejected: username '{}' already taken", req.username());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        Long userRoleId = roles.findByNameIgnoreCase("USER").stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Role USER is not configured"))
                .getId();

        UsersModel user = new UsersModel(null, req.username(), encoder.encode(req.password()),
                req.email(), req.fullName(), req.phone(), userRoleId);
        UsersModel saved = users.save(user);
        log.info("Registered user '{}' (id={})", saved.getUsername(), saved.getId());
        return UserResponse.from(saved);
    }
}