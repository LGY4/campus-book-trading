package com.booktrading.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.dto.LoginRequest;
import com.booktrading.dto.RegisterRequest;
import com.booktrading.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {

    User register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    User getUserById(Long id);

    void updateProfile(Long id, User user);

    void updateAvatar(Long id, String avatar);

    void changePassword(Long id, String oldPwd, String newPwd);

    IPage<User> listUsers(int page, int size, String keyword, String status, String role);

    List<User> searchUsers(String keyword, Long excludeUserId);
}
