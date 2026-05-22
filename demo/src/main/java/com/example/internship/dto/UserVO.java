package com.example.internship.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户视图对象
 */
@Data
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private String status;
    private LocalDateTime createTime;
    private String name; // 学生姓名 / 企业联系人 / 管理员编号
    private String companyName; // 企业名称
}
