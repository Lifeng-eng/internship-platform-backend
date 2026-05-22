package com.example.internship.controller;

import com.example.internship.config.WebSocketConfig;
import com.example.internship.dto.MessageVO;
import com.example.internship.dto.SendMessageRequest;
import com.example.internship.entity.Conversation;
import com.example.internship.entity.Message;
import com.example.internship.entity.User;
import com.example.internship.mapper.UserMapper;
import com.example.internship.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketHandler {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserMapper userMapper;

    public ChatWebSocketHandler(ChatService chatService, SimpMessagingTemplate messagingTemplate,
                                UserMapper userMapper) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.userMapper = userMapper;
    }

    @MessageMapping("/chat.send")
    public void handleSend(@Payload SendMessageRequest req, Principal principal) {
        WebSocketConfig.StompPrincipal sp = (WebSocketConfig.StompPrincipal) principal;
        Message msg = chatService.sendMessage(sp.userId(), req.getApplicationId(), req.getContent());

        MessageVO vo = buildMessageVO(msg);

        // 找到接收方并推送消息
        Conversation conv = chatService.getOrCreateConversation(req.getApplicationId());
        Long receiverId = sp.userId().equals(conv.getStudentId()) ? conv.getCompanyId() : conv.getStudentId();
        messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/chat", vo);
    }

    private MessageVO buildMessageVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setConversationId(m.getConversationId());
        vo.setSenderId(m.getSenderId());
        vo.setContent(m.getContent());
        vo.setIsRead(m.getIsRead());
        vo.setSendTime(m.getSendTime());
        User sender = userMapper.selectById(m.getSenderId());
        vo.setSenderName(sender != null ? sender.getUsername() : "未知");
        return vo;
    }
}
