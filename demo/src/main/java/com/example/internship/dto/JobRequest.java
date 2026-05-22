package com.example.internship.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 发布/编辑岗位请求
 */
@Data
public class JobRequest {
    @NotBlank(message = "岗位标题不能为空")
    private String title;

    @NotBlank(message = "工作地点不能为空")
    private String location;

    @NotBlank(message = "岗位描述不能为空")
    private String description;

    @NotBlank(message = "岗位要求不能为空")
    private String requirements;

    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
}
