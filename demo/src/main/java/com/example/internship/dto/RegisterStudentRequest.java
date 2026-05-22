package com.example.internship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 学生注册请求
 */
@Data
public class RegisterStudentRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "学号不能为空")
    private String studentId;

    @NotBlank(message = "专业不能为空")
    private String major;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String email;
    private String preferredJobs;
    private String preferredLocations;
}
