package com.example.internship.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 处理投递请求（接受/拒绝）
 */
@Data
public class HandleApplicationRequest {
    @NotBlank(message = "操作不能为空")
    private String action; // accept / reject
    private String handleComment; // 可选附言
}
