package com.example.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.chat.entity.Conversation;
import com.example.chat.entity.User;
import com.example.chat.mapper.ConversationMapper;
import com.example.chat.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class ConversationService extends ServiceImpl<ConversationMapper, Conversation> {

    @Autowired
    private MessageService messageService;
    
    @Autowired
    private UserMapper userMapper;

    public Conversation createSession(Long userId) {
        Conversation conversation = new Conversation();
        conversation.setSessionId(generateSessionId());
        conversation.setUserId(userId);
        conversation.setStatus(1);
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        save(conversation);
        return conversation;
    }

    public Conversation getBySessionId(String sessionId) {
        return getOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getSessionId, sessionId));
    }

    public void updateStatus(String sessionId, Integer status) {
        Conversation conversation = getBySessionId(sessionId);
        if (conversation != null) {
            conversation.setStatus(status);
            conversation.setUpdateTime(LocalDateTime.now());
            updateById(conversation);
        }
    }

    @Transactional
    public void deleteSession(String sessionId) {
        messageService.deleteBySessionId(sessionId);
        remove(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getSessionId, sessionId));
    }

    public java.util.List<Conversation> listOrderByUpdateTimeDesc() {
        return list(new LambdaQueryWrapper<Conversation>()
                .orderByDesc(Conversation::getUpdateTime));
    }

    public java.util.List<Conversation> listByUserId(Long userId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(Conversation::getUserId, userId);
        }
        wrapper.orderByDesc(Conversation::getUpdateTime);
        return list(wrapper);
    }

    public java.util.List<Conversation> listByStatus(Integer status) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Conversation::getStatus, status);
        }
        wrapper.orderByDesc(Conversation::getUpdateTime);
        List<Conversation> conversations = list(wrapper);
        
        fillUserNickname(conversations);
        
        return conversations;
    }
    
    public java.util.List<Conversation> listByAgentId(Long agentId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        if (agentId != null) {
            wrapper.eq(Conversation::getAgentId, agentId);
        }
        wrapper.orderByDesc(Conversation::getUpdateTime);
        List<Conversation> conversations = list(wrapper);
        
        fillUserNickname(conversations);
        
        return conversations;
    }
    
    public void assignAgent(String sessionId, Long agentId) {
        Conversation conversation = getBySessionId(sessionId);
        if (conversation != null) {
            conversation.setAgentId(agentId);
            conversation.setStatus(2);
            conversation.setUpdateTime(LocalDateTime.now());
            updateById(conversation);
        }
    }
    
    private void fillUserNickname(List<Conversation> conversations) {
        if (conversations == null || conversations.isEmpty()) {
            return;
        }
        
        Set<Long> userIds = conversations.stream()
                .map(Conversation::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        if (userIds.isEmpty()) {
            return;
        }
        
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> userNicknameMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
        
        for (Conversation conv : conversations) {
            if (conv.getUserId() != null) {
                conv.setNickname(userNicknameMap.get(conv.getUserId()));
            }
        }
    }

    public void acceptSession(String sessionId, Long agentId) {
        Conversation conversation = getBySessionId(sessionId);
        if (conversation != null) {
            conversation.setStatus(2);
            conversation.setUpdateTime(LocalDateTime.now());
            updateById(conversation);
        }
    }

    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}