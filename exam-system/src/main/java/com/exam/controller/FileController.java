package com.exam.controller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.exam.common.result.Result;

@RestController
@RequestMapping("/api/upload")
public class FileController {

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    @PostMapping("/image")
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的图片");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) originalName = "image.png";

        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex < 0) {
            return Result.error(400, "文件缺少扩展名，仅支持 jpg/jpeg/png/gif/webp/bmp 格式的图片");
        }
        String ext = originalName.substring(dotIndex).toLowerCase();
        Set<String> allowedExts = new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"));
        if (!allowedExts.contains(ext)) {
            return Result.error(400, "仅支持 jpg/jpeg/png/gif/webp/bmp 格式的图片");
        }

        String contentType = file.getContentType();
        Set<String> allowedMimes = new HashSet<>(Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"));
        if (contentType == null || !allowedMimes.contains(contentType)) {
            return Result.error(400, "文件内容类型不合法，仅支持图片格式");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过 5MB");
        }

        String fileName = "chat/" + UUID.randomUUID().toString().replace("-", "") + ext;
        File baseDir = new File(uploadPath).getAbsoluteFile();
        File dest = new File(baseDir, fileName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error(500, "图片上传失败：" + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("url", "/uploads/" + fileName);
        result.put("name", originalName);
        return Result.success("上传成功", result);
    }

    @PostMapping("/file")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) originalName = "file";

        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex < 0) {
            return Result.error(400, "文件缺少扩展名");
        }
        String ext = originalName.substring(dotIndex).toLowerCase();
        Set<String> allowedExts = new HashSet<>(Arrays.asList(
                ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
                ".pdf", ".txt", ".doc", ".docx", ".csv", ".xls", ".xlsx"));
        if (!allowedExts.contains(ext)) {
            return Result.error(400, "不支持的文件格式，支持: 图片/PDF/TXT/DOC/DOCX/CSV/XLS/XLSX");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过 10MB");
        }

        String fileName = "chat/" + UUID.randomUUID().toString().replace("-", "") + ext;
        File baseDir = new File(uploadPath).getAbsoluteFile();
        File dest = new File(baseDir, fileName);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error(500, "文件上传失败：" + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("url", "/uploads/" + fileName);
        result.put("name", originalName);
        return Result.success("上传成功", result);
    }
}
