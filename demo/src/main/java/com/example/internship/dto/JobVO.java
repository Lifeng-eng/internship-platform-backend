package com.example.internship.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 岗位视图对象（含企业名称等关联字段）
 */
@Data
public class JobVO {
    private Long id;
    private String title;
    private Long companyId;
    private String companyName;
    private String companyIntro;
    private String companyScale;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String location;
    private String description;
    private String requirements;
    private String status;
    private LocalDateTime publishTime;
    private LocalDateTime reviewTime;

    // 学生端附加字段
    private String applyStatus; // 当前学生对该岗位的投递状态
    private Long applicationId;
}
