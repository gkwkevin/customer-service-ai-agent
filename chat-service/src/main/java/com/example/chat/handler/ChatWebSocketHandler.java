package com.example.chat.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chat.entity.Message;
import com.example.chat.entity.User;
import com.example.chat.mapper.UserMapper;
import com.example.chat.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<Long, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private com.example.chat.service.ConversationService conversationService;

    @Value("${ai.service.url:http://localhost:8082}")
    private String aiServiceUrl;

    @Value("${ticket.service.url:http://localhost:8083}")
    private String ticketServiceUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ChatWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        String sessionIdAttr = (String) session.getAttributes().get("sessionId");
        Long agentId = (Long) session.getAttributes().get("agentId");
        
        if (agentId != null) {
            agentSessions.put(agentId, session);
            System.out.println("Agent WebSocket connected: " + session.getId() + ", agentId: " + agentId);
        } else {
            System.out.println("User WebSocket connected: " + session.getId() + ", sessionId: " + sessionIdAttr);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);

        String msgSessionId = (String) data.get("sessionId");
        String content = (String) data.get("content");
        Integer senderType = data.get("senderType") != null ? (Integer) data.get("senderType") : 1;
        Long senderId = data.get("senderId") != null ? Long.valueOf(data.get("senderId").toString()) : null;

        Message userMessage = messageService.saveMessage(msgSessionId, content, senderType, senderId);
        broadcastToSessionId(msgSessionId, userMessage);

        Map<String, Object> aiResult = callAiServiceWithFunction(msgSessionId, content);
        String type = (String) aiResult.get("type");

        if ("function_call".equals(type)) {
            String functionName = (String) aiResult.get("functionName");
            if ("createTicket".equals(functionName)) {
                Map<String, Object> ticketResult = createTicketAndAssignAgent(msgSessionId, content, senderId);
                
                if (ticketResult != null && ticketResult.get("success").equals(true)) {
                    Long assignedAgentId = Long.valueOf(ticketResult.get("agentId").toString());
                    String agentName = (String) ticketResult.get("agentName");
                    Long ticketId = Long.valueOf(ticketResult.get("ticketId").toString());
                    
                    conversationService.assignAgent(msgSessionId, assignedAgentId);
                    
                    String replyContent = "已为您创建工单 #" + ticketId + "，客服 " + agentName + " 将尽快为您服务。";
                    Message aiMessage = messageService.saveMessage(msgSessionId, replyContent, 2, null);
                    broadcastToSessionId(msgSessionId, aiMessage);
                    
                    notifyAgent(assignedAgentId, ticketId, msgSessionId, content);
                } else {
                    String replyContent = "抱歉，暂时没有可用的客服，请稍后再试。";
                    Message aiMessage = messageService.saveMessage(msgSessionId, replyContent, 2, null);
                    broadcastToSessionId(msgSessionId, aiMessage);
                }
            }
        } else {
            String replyContent = (String) aiResult.get("content");
            Message aiMessage = messageService.saveMessage(msgSessionId, replyContent, 2, null);
            broadcastToSessionId(msgSessionId, aiMessage);
        }
    }

    private Map<String, Object> callAiServiceWithFunction(String sessionId, String userMessage) {
        try {
            List<Message> historyMessages = messageService.getMessagesBySessionId(sessionId);
            
            List<Map<String, String>> history = new ArrayList<>();
            if (historyMessages != null && !historyMessages.isEmpty()) {
                int startIndex = Math.max(0, historyMessages.size() - 10);
                for (int i = startIndex; i < historyMessages.size(); i++) {
                    Message msg = historyMessages.get(i);
                    Map<String, String> historyItem = new HashMap<>();
                    historyItem.put("role", msg.getSenderType() == 1 ? "user" : "assistant");
                    historyItem.put("content", msg.getContent());
                    history.add(historyItem);
                }
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", userMessage);
            requestBody.put("history", history);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    aiServiceUrl + "/api/ai/chat/rag",
                    entity,
                    Map.class
            );

            if (response != null && response.get("data") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return data;
            }
        } catch (Exception e) {
            System.err.println("AI服务调用失败: " + e.getMessage());
        }
        
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("type", "text");
        fallback.put("content", "抱歉，我暂时无法回答您的问题，请稍后再试。");
        return fallback;
    }

    private Map<String, Object> createTicketAndAssignAgent(String sessionId, String content, Long userId) {
        try {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(User::getRole, 2)
                   .eq(User::getStatus, 1);
            List<User> agents = userMapper.selectList(wrapper);
            
            if (agents == null || agents.isEmpty()) {
                return null;
            }
            
            Random random = new Random();
            User assignedAgent = agents.get(random.nextInt(agents.size()));
            
            Map<String, Object> ticketData = new HashMap<>();
            ticketData.put("sessionId", sessionId);
            ticketData.put("userId", userId);
            ticketData.put("title", "用户转人工请求");
            ticketData.put("content", content);
            ticketData.put("status", 1);
            ticketData.put("priority", 2);
            ticketData.put("assigneeId", assignedAgent.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(ticketData, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    ticketServiceUrl + "/api/ticket",
                    entity,
                    Map.class
            );

            if (response != null && response.get("data") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ticket = (Map<String, Object>) response.get("data");
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("agentId", assignedAgent.getId());
                result.put("agentName", assignedAgent.getNickname());
                result.put("ticketId", ticket.get("id"));
                return result;
            }
        } catch (Exception e) {
            System.err.println("创建工单失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private void notifyAgent(Long agentId, Long ticketId, String sessionId, String userMessage) {
        try {
            WebSocketSession agentSession = agentSessions.get(agentId);
            
            if (agentSession != null && agentSession.isOpen()) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "new_ticket");
                notification.put("ticketId", ticketId);
                notification.put("sessionId", sessionId);
                notification.put("message", "您有新的工单，请处理");
                notification.put("userMessage", userMessage);
                notification.put("timestamp", System.currentTimeMillis());
                
                String json = objectMapper.writeValueAsString(notification);
                agentSession.sendMessage(new TextMessage(json));
                System.out.println("Notification sent to agent " + agentId + ": " + json);
            } else {
                System.out.println("Agent " + agentId + " is not online, notification not sent");
            }
        } catch (Exception e) {
            System.err.println("推送通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        
        Long agentId = (Long) session.getAttributes().get("agentId");
        if (agentId != null) {
            agentSessions.remove(agentId);
        }
        
        System.out.println("WebSocket disconnected: " + session.getId());
    }

    public void broadcastToSession(String sessionId, Message message) throws Exception {
        String response = objectMapper.writeValueAsString(message);
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(response));
            }
        }
    }

    public void broadcastToSessionId(String targetSessionId, Message message) throws Exception {
        String response = objectMapper.writeValueAsString(message);
        System.out.println("=== Broadcasting ===");
        System.out.println("Target sessionId: " + targetSessionId);
        System.out.println("Message: " + response);
        System.out.println("Total connected sessions: " + sessions.size());
        
        boolean found = false;
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                String sessionIdAttr = (String) session.getAttributes().get("sessionId");
                System.out.println("Checking session " + session.getId() + " with sessionId: " + sessionIdAttr);
                
                if (targetSessionId.equals(sessionIdAttr)) {
                    System.out.println("-> Sending to user session: " + session.getId());
                    session.sendMessage(new TextMessage(response));
                    found = true;
                } else if (sessionIdAttr != null && sessionIdAttr.startsWith("agent_")) {
                    System.out.println("-> Sending to agent session: " + session.getId());
                    session.sendMessage(new TextMessage(response));
                }
            }
        }
        if (!found) {
            System.out.println("WARNING: No matching user session found for sessionId: " + targetSessionId);
        }
        System.out.println("=== Broadcast complete ===");
    }

    public void sendNotificationToAgent(Long agentId, Map<String, Object> notification) throws Exception {
        WebSocketSession agentSession = agentSessions.get(agentId);
        if (agentSession != null && agentSession.isOpen()) {
            String json = objectMapper.writeValueAsString(notification);
            agentSession.sendMessage(new TextMessage(json));
        }
    }
}
