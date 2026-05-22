package com.example.internship.controller;

import com.example.internship.common.Result;
import com.example.internship.common.SecurityUtils;
import com.example.internship.dto.ChangePasswordRequest;
import com.example.internship.dto.UpdateProfileRequest;
import com.example.internship.service.FileStorageService;
import com.example.internship.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心控制器
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final FileStorageService fileStorageService;

    public ProfileController(ProfileService profileService, FileStorageService fileStorageService) {
        this.profileService = profileService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 获取个人信息
     */
    @GetMapping
    public Result<?> getProfile() {
        return Result.success(profileService.getProfile());
    }

    /**
     * 更新个人信息
     */
    @PutMapping
    public Result<?> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        profileService.updateProfile(req);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        profileService.changePassword(req);
        return Result.success();
    }

    /**
     * 上传简历（学生）
     */
    @PostMapping("/resume")
    public Result<?> uploadResume(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 先获取旧简历路径，上传成功后删除
        var profile = profileService.getProfile();
        String oldResumePath = (String) profile.get("resumePath");

        String storedName = fileStorageService.upload(file);

        // 删除旧文件
        if (oldResumePath != null && !oldResumePath.isEmpty()) {
            fileStorageService.delete(oldResumePath);
        }

        profileService.updateResumePath(userId, storedName);
        return Result.success(storedName);
    }

    /**
     * 下载简历（学生下载自己 / 企业下载投递给自己的简历）
     */
    @GetMapping("/resume")
    public ResponseEntity<ByteArrayResource> downloadResume(@RequestParam(required = false) String path) {
        // 安全校验由业务层处理
        byte[] data = fileStorageService.read(path);
        ByteArrayResource resource = new ByteArrayResource(data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    /**
     * 获取简历信息
     */
    @GetMapping("/resume/info")
    public Result<?> getResumeInfo() {
        var profile = profileService.getProfile();
        String resumePath = (String) profile.get("resumePath");
        return Result.success(resumePath != null ? java.util.Map.of("resumePath", resumePath) : null);
    }

    /**
     * 删除简历
     */
    @DeleteMapping("/resume")
    public Result<?> deleteResume() {
        Long userId = SecurityUtils.getCurrentUserId();
        var profile = profileService.getProfile();
        String resumePath = (String) profile.get("resumePath");
        if (resumePath != null) {
            fileStorageService.delete(resumePath);
            profileService.updateResumePath(userId, null);
        }
        return Result.success();
    }
}
