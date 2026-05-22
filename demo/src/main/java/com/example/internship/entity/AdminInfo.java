package com.example.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 管理员信息表
 */
@Data
@TableName("admin_info")
public class AdminInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String adminId;
    private Boolean mustChangePassword;
}
