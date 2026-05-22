package com.example.internship.dto;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String role;
    private String name;
    private Integer mustChangePassword; // 管理员首次登录=1

    public LoginResponse(String accessToken, String refreshToken, Long id, String role, String name) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.role = role;
        this.name = name;
    }
}
