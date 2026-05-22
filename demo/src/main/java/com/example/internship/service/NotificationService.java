package com.example.internship.service;

import com.example.internship.entity.Notification;
import com.example.internship.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 通知服务 - 创建通知、标记已读
 */
@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 创建通知
     */
    public void createNotification(Long receiverId, Long senderId, String content, String type) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setContent(content);
        notification.setType(type);
        notification.setStatus("unread");
        notification.setSendTime(LocalDateTime.now());
        notificationMapper.insert(notification);
    }
}
