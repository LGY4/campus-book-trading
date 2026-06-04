package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.dto.LoginRequest;
import com.booktrading.dto.RegisterRequest;
import com.booktrading.entity.User;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.UserMapper;
import com.booktrading.security.JwtTokenProvider;
import com.booktrading.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (count(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        if (StringUtils.hasText(request.getPhone())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, request.getPhone());
            if (count(wrapper) > 0) {
                throw new BusinessException("手机号已被注册");
            }
        }

        if (StringUtils.hasText(request.getEmail())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, request.getEmail());
            if (count(wrapper) > 0) {
                throw new BusinessException("邮箱已被注册");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole("STUDENT");
        user.setStatus("ACTIVE");
        user.setReputationScore(new java.math.BigDecimal("5.0"));
        save(user);
        return user;
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = getOne(wrapper);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @Override
    public User getUserById(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(Long id, User user) {
        User existing = getUserById(id);
        if (StringUtils.hasText(user.getNickname())) {
            existing.setNickname(user.getNickname());
        }
        if (StringUtils.hasText(user.getPhone())) {
            existing.setPhone(user.getPhone());
        }
        if (StringUtils.hasText(user.getEmail())) {
            existing.setEmail(user.getEmail());
        }
        updateById(existing);
    }

    @Override
    public void updateAvatar(Long id, String avatar) {
        User user = getUserById(id);
        user.setAvatar(avatar);
        updateById(user);
    }

    @Override
    public void changePassword(Long id, String oldPwd, String newPwd) {
        User user = getUserById(id);
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        updateById(user);
    }

    @Override
    public IPage<User> listUsers(int page, int size, String keyword, String status, String role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getStatus, status);
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<User> searchUsers(String keyword, Long excludeUserId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, "ACTIVE");
        if (excludeUserId != null) {
            wrapper.ne(User::getId, excludeUserId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword));
        }
        wrapper.last("LIMIT 20");
        return list(wrapper);
    }
}
