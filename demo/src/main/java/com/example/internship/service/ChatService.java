package com.example.internship.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.internship.common.SecurityUtils;
import com.example.internship.dto.ConversationVO;
import com.example.internship.dto.MessageVO;
import com.example.internship.entity.*;
import com.example.internship.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ApplicationMapper applicationMapper;
    private final JobMapper jobMapper;
    private final UserMapper userMapper;

    public ChatService(ConversationMapper conversationMapper, MessageMapper messageMapper,
                       ApplicationMapper applicationMapper, JobMapper jobMapper, UserMapper userMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.applicationMapper = applicationMapper;
        this.jobMapper = jobMapper;
        this.userMapper = userMapper;
    }

    /** 获取当前用户的会话列表 */
    public List<ConversationVO> getConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        var query = new LambdaQueryWrapper<Conversation>();
        if ("student".equals(role)) {
            query.eq(Conversation::getStudentId, userId);
        } else {
            query.eq(Conversation::getCompanyId, userId);
        }
        query.orderByDesc(Conversation::getLastMessageTime);

        List<Conversation> conversations = conversationMapper.selectList(query);
        if (conversations.isEmpty()) return Collections.emptyList();

        // 收集关联数据
        Set<Long> jobIds = conversations.stream().map(Conversation::getJobId).collect(Collectors.toSet());
        Set<Long> studentIds = conversations.stream().map(Conversation::getStudentId).collect(Collectors.toSet());
        Set<Long> companyIds = conversations.stream().map(Conversation::getCompanyId).collect(Collectors.toSet());
        Set<Long> allUserIds = new HashSet<>();
        allUserIds.addAll(studentIds);
        allUserIds.addAll(companyIds);

        Map<Long, Job> jobMap = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));
        Map<Long, User> userMap = userMapper.selectBatchIds(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return conversations.stream().map(c -> {
            ConversationVO vo = new ConversationVO();
            vo.setId(c.getId());
            vo.setApplicationId(c.getApplicationId());
            vo.setStudentId(c.getStudentId());
            vo.setCompanyId(c.getCompanyId());
            vo.setJobId(c.getJobId());
            vo.setLastMessage(c.getLastMessage());
            vo.setLastMessageTime(c.getLastMessageTime());
            vo.setCreateTime(c.getCreateTime());

            Job job = jobMap.get(c.getJobId());
            if (job != null) vo.setJobTitle(job.getTitle());

            User companyUser = userMap.get(c.getCompanyId());
            if (companyUser != null) vo.setCompanyName(companyUser.getUsername());

            User studentUser = userMap.get(c.getStudentId());
            if (studentUser != null) vo.setStudentName(studentUser.getUsername());

            // 计算未读数
            long unread = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                    .eq(Message::getConversationId, c.getId())
                    .eq(Message::getIsRead, false)
                    .ne(Message::getSenderId, userId));
            vo.setUnreadCount(unread);

            return vo;
        }).collect(Collectors.toList());
    }

    /** 获取会话的消息历史 */
    public List<MessageVO> getMessages(Long conversationId, int page, int size) {
        Long userId = SecurityUtils.getCurrentUserId();

        Page<Message> pageObj = new Page<>(page, size);
        var query = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByDesc(Message::getSendTime);
        Page<Message> result = messageMapper.selectPage(pageObj, query);

        // 收集发送者ID
        Set<Long> senderIds = result.getRecords().stream()
                .map(Message::getSenderId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 倒序排列（最新在底部）
        List<MessageVO> vos = new ArrayList<>();
        for (int i = result.getRecords().size() - 1; i >= 0; i--) {
            Message m = result.getRecords().get(i);
            MessageVO vo = new MessageVO();
            vo.setId(m.getId());
            vo.setConversationId(m.getConversationId());
            vo.setSenderId(m.getSenderId());
            vo.setContent(m.getContent());
            vo.setIsRead(m.getIsRead());
            vo.setSendTime(m.getSendTime());
            User sender = userMap.get(m.getSenderId());
            vo.setSenderName(sender != null ? sender.getUsername() : "未知");
            vos.add(vo);
        }
        return vos;
    }

    /** 获取总未读消息数 */
    public long getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        var convQuery = new LambdaQueryWrapper<Conversation>();
        if ("student".equals(role)) {
            convQuery.eq(Conversation::getStudentId, userId);
        } else {
            convQuery.eq(Conversation::getCompanyId, userId);
        }
        List<Long> convIds = conversationMapper.selectList(convQuery).stream()
                .map(Conversation::getId).toList();
        if (convIds.isEmpty()) return 0;

        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .in(Message::getConversationId, convIds)
                .eq(Message::getIsRead, false)
                .ne(Message::getSenderId, userId));
    }

    /** 获取或创建会话（发消息时调用） */
    @Transactional
    public Conversation getOrCreateConversation(Long applicationId) {
        var existing = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getApplicationId, applicationId));
        if (existing != null) return existing;

        Application app = applicationMapper.selectById(applicationId);
        if (app == null) throw new RuntimeException("投递记录不存在");

        Job job = jobMapper.selectById(app.getJobId());
        if (job == null) throw new RuntimeException("岗位不存在");

        Conversation conv = new Conversation();
        conv.setApplicationId(applicationId);
        conv.setStudentId(app.getStudentId());
        conv.setCompanyId(job.getCompanyId());
        conv.setJobId(app.getJobId());
        conv.setCreateTime(LocalDateTime.now());
        conversationMapper.insert(conv);
        return conv;
    }

    /** 发送消息并更新会话预览（REST 调用） */
    @Transactional
    public Message sendMessage(Long applicationId, String content) {
        return sendMessage(SecurityUtils.getCurrentUserId(), applicationId, content);
    }

    /** 发送消息并更新会话预览（WebSocket 调用，显式传入 senderId） */
    @Transactional
    public Message sendMessage(Long senderId, Long applicationId, String content) {
        Conversation conv = getOrCreateConversation(applicationId);

        Message msg = new Message();
        msg.setConversationId(conv.getId());
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setIsRead(false);
        msg.setSendTime(LocalDateTime.now());
        messageMapper.insert(msg);

        // 更新会话预览
        conv.setLastMessage(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        conv.setLastMessageTime(msg.getSendTime());
        conversationMapper.updateById(conv);

        return msg;
    }

    /** 标记会话中的消息为已读 */
    @Transactional
    public void markRead(Long conversationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        var query = new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getIsRead, false)
                .ne(Message::getSenderId, userId);
        List<Message> unreadMessages = messageMapper.selectList(query);
        for (Message msg : unreadMessages) {
            msg.setIsRead(true);
            messageMapper.updateById(msg);
        }
    }
}
