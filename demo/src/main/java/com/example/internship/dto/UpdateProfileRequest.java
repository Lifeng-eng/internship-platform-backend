package com.example.internship.dto;

import lombok.Data;

/**
 * 更新个人信息请求（通用，根据角色选填不同字段）
 */
@Data
public class UpdateProfileRequest {
    // 学生字段
    private String name;
    private String major;
    private String preferredJobs;
    private String preferredLocations;

    // 企业字段
    private String contactPerson;
    private String companyIntro;
    private String companyScale;

    // 公共字段
    private String email;
    private String phone;
}
