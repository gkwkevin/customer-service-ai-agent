package com.example.chat.controller;

import com.example.chat.common.Result;
import com.example.chat.entity.Conversation;
import com.example.chat.entity.Message;
import com.example.chat.handler.ChatWebSocketHandler;
import com.example.chat.service.ConversationService;
import com.example.chat.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @PostMapping("/session")
    public Result<Map<String, Object>> createSession(@RequestBody(required = false) Map<String, Long> params) {
        Long userId = params != null ? params.get("userId") : null;
        Conversation conversation = conversationService.createSession(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", conversation.getSessionId());
        data.put("createTime", conversation.getCreateTime());
        return Result.success(data);
    }

    @PostMapping("/message")
    public Result<Message> sendMessage(@RequestBody Map<String, Object> params) {
        String sessionId = (String) params.get("sessionId");
        String content = (String) params.get("content");
        Integer senderType = params.get("senderType") != null ? (Integer) params.get("senderType") : 1;
        Long senderId = params.get("senderId") != null ? Long.valueOf(params.get("senderId").toString()) : null;
        
        Message message = messageService.saveMessage(sessionId, content, senderType, senderId);
        
        if (senderType == 2) {
            try {
                webSocketHandler.broadcastToSessionId(sessionId, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return Result.success(message);
    }

    @GetMapping("/message/list")
    public Result<Map<String, Object>> getMessageList(
            @RequestParam(value = "sessionId") String sessionId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        List<Message> messages = messageService.getMessagesBySessionId(sessionId);
        Map<String, Object> data = new HashMap<>();
        data.put("total", messages.size());
        data.put("list", messages);
        return Result.success(data);
    }

    @GetMapping("/session/list")
    public Result<Map<String, Object>> getSessionList(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        List<Conversation> conversations = conversationService.listByUserId(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("total", conversations.size());
        data.put("list", conversations);
        return Result.success(data);
    }

    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable("sessionId") String sessionId) {
        conversationService.deleteSession(sessionId);
        return Result.success();
    }

    @GetMapping("/agent/sessions")
    public Result<Map<String, Object>> getAgentSessions(
            @RequestParam(value = "agentId", required = false) Long agentId,
            @RequestParam(value = "status", required = false) Integer status) {
        List<Conversation> conversations;
        if (agentId != null) {
            conversations = conversationService.listByAgentId(agentId);
        } else {
            conversations = conversationService.listByStatus(status);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("total", conversations.size());
        data.put("list", conversations);
        return Result.success(data);
    }

    @PostMapping("/agent/accept/{sessionId}")
    public Result<Void> acceptSession(@PathVariable("sessionId") String sessionId, @RequestParam("agentId") Long agentId) {
        conversationService.acceptSession(sessionId, agentId);
        return Result.success();
    }

    @PostMapping("/agent/close/{sessionId}")
    public Result<Void> closeSession(@PathVariable("sessionId") String sessionId) {
        conversationService.updateStatus(sessionId, 3);
        return Result.success();
    }
}