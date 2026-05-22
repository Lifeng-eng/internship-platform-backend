package com.example.internship.dto;

import lombok.Data;

/**
 * 投递岗位请求
 */
@Data
public class ApplicationRequest {
    private Long jobId;
    private String applyComment; // 可选，投递附言
}
