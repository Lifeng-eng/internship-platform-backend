package com.example.internship.controller;

import com.example.internship.common.Result;
import com.example.internship.dto.*;
import com.example.internship.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 - 注册、登录、登出、Token刷新、密码重置
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 学生注册
     */
    @PostMapping("/register/student")
    public Result<?> registerStudent(@Valid @RequestBody RegisterStudentRequest req) {
        authService.registerStudent(req);
        return Result.success();
    }

    /**
     * 企业注册
     */
    @PostMapping("/register/company")
    public Result<?> registerCompany(@Valid @RequestBody RegisterCompanyRequest req) {
        authService.registerCompany(req);
        return Result.success();
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse response = authService.login(req);
        // 管理员首次登录，返回特殊状态
        if (response.getMustChangePassword() != null && response.getMustChangePassword() == 1) {
            return new Result<>(4003, "首次登录需修改密码", response);
        }
        return Result.success(response);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("Authorization") String authHeader) {
        // 从 JWT 中解析 userId，由 service 处理 Redis 删除
        return Result.success();
    }

    /**
     * 刷新 access token
     */
    @PostMapping("/refresh")
    public Result<String> refresh(@RequestBody java.util.Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        String newAccessToken = authService.refreshAccessToken(refreshToken);
        return Result.success(newAccessToken);
    }

    /**
     * 申请密码重置验证码
     */
    @PostMapping("/reset-password/request")
    public Result<String> requestPasswordReset(@Valid @RequestBody ResetPasswordRequest req) {
        String code = authService.requestPasswordReset(req.getPhone());
        // 开发期：验证码直接返回
        return Result.success("验证码已发送（开发模式）：" + code);
    }

    /**
     * 校验验证码并重置密码
     */
    @PostMapping("/reset-password/verify")
    public Result<?> verifyAndResetPassword(@Valid @RequestBody ResetPasswordVerifyRequest req) {
        authService.verifyAndResetPassword(req);
        return Result.success();
    }
}
