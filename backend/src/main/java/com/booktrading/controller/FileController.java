package com.booktrading.controller;

import com.booktrading.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Value("${file.upload-path:uploads/}")
    private String uploadDir;

    private String getAbsoluteUploadDir() {
        File dir = new File(uploadDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), uploadDir);
        }
        return dir.getAbsolutePath();
    }

    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/jpg"
    ));

    private static final Set<String> ALLOWED_EXTS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif"
    ));

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return Result.error("仅支持 jpg, png, gif 格式");
        }

        if (file.getSize() > MAX_SIZE) {
            return Result.error("文件大小不能超过10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        if (!ALLOWED_EXTS.contains(ext)) {
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
        return Result.ok(url);
    }
}
