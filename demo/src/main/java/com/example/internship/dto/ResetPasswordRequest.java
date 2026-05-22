package com.example.internship.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 申请密码重置验证码
 */
@Data
public class ResetPasswordRequest {
    @NotBlank(message = "手机号不能为空")
    private String phone;
}
