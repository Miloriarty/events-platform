package org.example.user.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.user.dto.UserResponse;
import org.example.user.service.UsersService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователи")
@SecurityRequirement(name = "BearerAuth")
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    public Page<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        return usersService.findPage(page, size).map(UserResponse::from);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return UserResponse.from(usersService.findById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        usersService.delete(id);
    }
}