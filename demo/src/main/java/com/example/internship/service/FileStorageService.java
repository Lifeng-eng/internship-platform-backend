package com.example.internship.service;

import com.example.internship.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件存储服务 - 简历上传/下载/删除
 * 开发期使用本地文件夹，生产环境可切换为 OSS
 */
@Service
public class FileStorageService {

    private final Path uploadDir;
    private final long maxSize;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir,
                               @Value("${file.max-size}") long maxSize) {
        this.uploadDir = Paths.get(uploadDir);
        this.maxSize = maxSize;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadDir, e);
        }
    }

    /**
     * 上传文件，返回存储文件名
     */
    public String upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过 5MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("仅支持 PDF 格式文件");
        }

        // 文件名随机化，避免冲突
        String storedName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
        try {
            Files.copy(file.getInputStream(), uploadDir.resolve(storedName));
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }
        return storedName;
    }

    /**
     * 删除文件
     */
    public void delete(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;
        try {
            Files.deleteIfExists(uploadDir.resolve(fileName));
        } catch (IOException e) {
            // 忽略删除失败
        }
    }

    /**
     * 获取文件字节数组
     */
    public byte[] read(String fileName) {
        try {
            return Files.readAllBytes(uploadDir.resolve(fileName));
        } catch (IOException e) {
            throw new BusinessException(404, "文件不存在");
        }
    }

    /**
     * 获取文件路径
     */
    public Path getFilePath(String fileName) {
        return uploadDir.resolve(fileName);
    }
}
