package com.example.internship.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.internship.common.PageResult;
import com.example.internship.common.Result;
import com.example.internship.common.SecurityUtils;
import com.example.internship.entity.Notification;
import com.example.internship.mapper.NotificationMapper;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 我的通知列表
     */
    @GetMapping
    public Result<?> listNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        var query = new LambdaQueryWrapper<Notification>()
                .and(w -> w.eq(Notification::getReceiverId, userId)
                        .or().isNull(Notification::getReceiverId))
                .orderByDesc(Notification::getSendTime);

        // 管理员看到 review 类通知，普通用户看自己的
        if (!"admin".equals(role)) {
            query.eq(Notification::getReceiverId, userId);
        }

        Page<Notification> pageObj = new Page<>(page, size);
        Page<Notification> result = notificationMapper.selectPage(pageObj, query);
        return Result.success(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    /**
     * 未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<?> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        var query = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getStatus, "unread");
        if ("admin".equals(role)) {
            query.and(w -> w.eq(Notification::getReceiverId, userId)
                    .or().isNull(Notification::getReceiverId));
        } else {
            query.eq(Notification::getReceiverId, userId);
        }

        long count = notificationMapper.selectCount(query);
        return Result.success(java.util.Map.of("count", count));
    }

    /**
     * 标记单条已读
     */
    @PutMapping("/{id}/read")
    public Result<?> markRead(@PathVariable Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification != null) {
            notification.setStatus("read");
            notificationMapper.updateById(notification);
        }
        return Result.success();
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read-all")
    public Result<?> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationMapper.markAllRead(userId);
        return Result.success();
    }
}
