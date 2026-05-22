package com.example.internship.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long id;
    private Long applicationId;
    private Long studentId;
    private Long companyId;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String studentName;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private LocalDateTime createTime;
}
