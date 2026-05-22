package com.example.internship.controller;

import com.example.internship.common.Result;
import com.example.internship.dto.MessageVO;
import com.example.internship.dto.SendMessageRequest;
import com.example.internship.entity.Message;
import com.example.internship.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 获取或创建会话（用于聊天入口） */
    @PostMapping("/conversations")
    public Result<Map<String, Long>> getOrCreate(@RequestBody Map<String, Long> body) {
        Long applicationId = body.get("applicationId");
        Long convId = chatService.getOrCreateConversation(applicationId).getId();
        return Result.success(Map.of("conversationId", convId));
    }

    /** 会话列表 */
    @GetMapping("/conversations")
    public Result<List<?>> listConversations() {
        return Result.success(chatService.getConversations());
    }

    /** 消息历史 */
    @GetMapping("/conversations/{id}/messages")
    public Result<List<MessageVO>> listMessages(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return Result.success(chatService.getMessages(id, page, size));
    }

    /** 发送消息（REST 备用） */
    @PostMapping("/send")
    public Result<MessageVO> sendMessage(@Valid @RequestBody SendMessageRequest req) {
        Message msg = chatService.sendMessage(req.getApplicationId(), req.getContent());
        MessageVO vo = toMessageVO(msg);
        return Result.success(vo);
    }

    /** 未读消息总数 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.success(Map.of("count", chatService.getUnreadCount()));
    }

    /** 标记会话已读 */
    @PutMapping("/conversations/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        chatService.markRead(id);
        return Result.success();
    }

    private MessageVO toMessageVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setConversationId(m.getConversationId());
        vo.setSenderId(m.getSenderId());
        vo.setContent(m.getContent());
        vo.setIsRead(m.getIsRead());
        vo.setSendTime(m.getSendTime());
        return vo;
    }
}
