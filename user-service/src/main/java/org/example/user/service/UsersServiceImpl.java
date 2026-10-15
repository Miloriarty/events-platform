package org.example.user.service;

import org.example.user.model.UsersModel;
import org.example.user.repository.UsersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersServiceImpl implements UsersService {

    private final UsersRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsersServiceImpl(
            UsersRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UsersModel> findAll() {
        return repository.findAll();
    }

    @Override
    public UsersModel create(
            UsersModel user
    ) {

        user.setPassword(
                passwordEncoder.encode(
                        user.getPassword()
                )
        );

        return repository.save(user);
    }

    @Override
    public UsersModel update(
            UsersModel user
    ) {

        if (!repository.existsById(user.getId())) {
            throw new RuntimeException("User not found");
        }

        if (!user.getPassword().startsWith("$2")) {

            user.setPassword(
                    passwordEncoder.encode(
                            user.getPassword()
                    )
            );
        }

        return repository.save(user);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public UsersModel findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public List<UsersModel> findByUsername(String username) {
        return repository.findByUsernameIgnoreCase(username);
    }

    @Override
    public List<UsersModel> findByRoleId(Long roleId) {
        return repository.findByRoleId(roleId);
    }

    @Override
    public Page<UsersModel> findPage(int page, int size) {
        return repository.findAll(PageRequest.of(page, size));
    }
}