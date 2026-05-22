package com.example.internship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 校验验证码并重置密码
 */
@Data
public class ResetPasswordVerifyRequest {
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String newPassword;
}
