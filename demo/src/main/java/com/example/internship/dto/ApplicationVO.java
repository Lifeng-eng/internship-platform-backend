package com.example.internship.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 投递视图对象
 */
@Data
public class ApplicationVO {
    private Long id;
    private Long studentId;
    private Long jobId;
    private String status;
    private String resumePath;
    private String applyComment;
    private String handleComment;
    private LocalDateTime applyTime;
    private LocalDateTime handleTime;

    // 关联字段
    private String jobTitle;
    private String companyName;
    private String studentName;
    private String studentIdNum; // 学号
    private String major;
}
