package com.example.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知表
 */
@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiverId;
    private Long senderId;
    private String content;
    private String type; // application / review
    private String status; // unread / read
    private LocalDateTime sendTime;
}
