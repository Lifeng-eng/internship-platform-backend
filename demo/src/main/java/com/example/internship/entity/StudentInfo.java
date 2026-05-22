package com.example.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 学生信息表
 */
@Data
@TableName("student_info")
public class StudentInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String studentId;
    private String name;
    private String major;
    private String preferredJobs;
    private String preferredLocations;
    private String resumePath;
}
