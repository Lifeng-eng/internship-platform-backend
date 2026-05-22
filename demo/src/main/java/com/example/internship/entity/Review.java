package com.example.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审核记录表
 */
@Data
@TableName("review")
public class Review {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private Long adminId;
    private LocalDateTime reviewTime;
    private String result; // pass / reject
    private String comment;
}
