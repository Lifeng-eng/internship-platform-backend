package com.example.internship.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审核岗位请求
 */
@Data
public class ReviewRequest {
    @NotBlank(message = "审核结果不能为空")
    private String result; // pass / reject
    private String comment; // 拒绝时必填（业务层校验）
}
