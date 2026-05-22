package com.example.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 投递表
 */
@Data
@TableName("application")
public class Application {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long jobId;
    private String status; // pending / accepted / rejected / cancelled
    private String resumePath;
    private String applyComment;
    private String handleComment;
    private LocalDateTime applyTime;
    private LocalDateTime handleTime;
    private Long handlerId;
}
