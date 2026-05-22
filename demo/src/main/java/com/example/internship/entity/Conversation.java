package com.example.internship.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private Long studentId;
    private Long companyId;
    private Long jobId;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
