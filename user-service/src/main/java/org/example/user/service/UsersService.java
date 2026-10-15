package org.example.user.service;

import org.example.user.model.UsersModel;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UsersService {

    List<UsersModel> findAll();

    UsersModel create(UsersModel user);

    UsersModel update(UsersModel user);

    void delete(Long id);

    UsersModel findById(Long id);

    List<UsersModel> findByUsername(String username);

    List<UsersModel> findByRoleId(Long roleId);

    Page<UsersModel> findPage(int page, int size);
}