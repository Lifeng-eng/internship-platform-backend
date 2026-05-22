package com.example.internship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 企业注册请求
 */
@Data
public class RegisterCompanyRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String password;

    @NotBlank(message = "企业名称不能为空")
    private String companyName;

    @NotBlank(message = "联系人不能为空")
    private String contactPerson;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String email;
    private String companyIntro;
    private String companyScale;
}
