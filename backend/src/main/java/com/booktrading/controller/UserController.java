package com.booktrading.controller;

import com.booktrading.aspect.LogOperation;
import com.booktrading.dto.Result;
import com.booktrading.entity.Address;
import com.booktrading.entity.User;
import com.booktrading.security.CurrentUser;
import com.booktrading.service.AddressService;
import com.booktrading.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private CurrentUser currentUser;

    @Value("${file.upload-path:uploads/}")
    private String uploadDir;

    private String getAbsoluteUploadDir() {
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), uploadDir);
        }
        return dir.getAbsolutePath();
    }

    @GetMapping("/profile")
    public Result<?> getProfile() {
        return Result.ok(userService.getUserById(currentUser.getId()));
    }

    @PutMapping("/profile")
    @LogOperation(module = "用户", action = "修改资料")
    public Result<?> updateProfile(@RequestBody User user) {
        // Only allow updating safe fields
        User safe = new User();
        safe.setNickname(user.getNickname());
        safe.setPhone(user.getPhone());
        safe.setEmail(user.getEmail());
        userService.updateProfile(currentUser.getId(), safe);
        return Result.ok("更新成功");
    }

    @PostMapping("/avatar")
    @LogOperation(module = "用户", action = "上传头像")
    public Result<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("仅支持图片格式");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件大小不能超过10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        if (!".jpg".equals(ext) && !".jpeg".equals(ext) && !".png".equals(ext) && !".gif".equals(ext)) {
            return Result.error("仅支持 jpg, png, gif 格式");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        String absoluteDir = getAbsoluteUploadDir();
        File dir = new File(absoluteDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            file.transferTo(new File(dir, filename));
        } catch (IOException e) {
            return Result.error("上传失败: " + e.getMessage());
        }

        String url = "/uploads/" + filename;
        userService.updateAvatar(currentUser.getId(), url);
        return Result.ok(url);
    }

    @PutMapping("/password")
    @LogOperation(module = "用户", action = "修改密码")
    public Result<?> changePassword(@RequestBody Map<String, String> body) {
        String oldPwd = body.getOrDefault("oldPwd", body.get("oldPassword"));
        String newPwd = body.getOrDefault("newPwd", body.get("newPassword"));
        userService.changePassword(currentUser.getId(), oldPwd, newPwd);
        return Result.ok("密码修改成功");
    }

    @GetMapping("/search")
    public Result<?> searchUsers(@RequestParam String keyword) {
        Long currentUserId = currentUser.getId();
        List<User> users = userService.searchUsers(keyword, currentUserId);
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("nickname", u.getNickname());
            m.put("avatar", u.getAvatar());
            result.add(m);
        }
        return Result.ok(result);
    }

    // ---- Address sub-routes ----

    @GetMapping("/address/list")
    public Result<?> listAddresses() {
        return Result.ok(addressService.getMyAddresses(currentUser.getId()));
    }

    @GetMapping("/address/{id}")
    public Result<?> getAddress(@PathVariable Long id) {
        Address address = addressService.getById(id);
        if (address == null) {
            return Result.error("地址不存在");
        }
        return Result.ok(address);
    }

    @PostMapping("/address/create")
    public Result<?> createAddress(@RequestBody Address address) {
        address.setUserId(currentUser.getId());
        Address saved = addressService.createAddress(address);
        return Result.ok(saved);
    }

    @PutMapping("/address/update")
    public Result<?> updateAddress(@RequestBody Address address) {
        address.setUserId(currentUser.getId());
        addressService.updateAddress(address);
        return Result.ok("更新成功");
    }

    @DeleteMapping("/address/{id}")
    public Result<?> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id, currentUser.getId());
        return Result.ok("删除成功");
    }

    @PutMapping("/address/default/{id}")
    public Result<?> setDefaultAddress(@PathVariable Long id) {
        addressService.setDefault(id, currentUser.getId());
        return Result.ok("设置成功");
    }
}
