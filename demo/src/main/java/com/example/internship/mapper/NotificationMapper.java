package com.example.internship.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.internship.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 全部标记已读
     */
    @Update("UPDATE notification SET status = 'read' WHERE receiver_id = #{userId} AND status = 'unread'")
    int markAllRead(@Param("userId") Long userId);
}
