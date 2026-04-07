package com.example.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.chat.entity.Message;
import com.example.chat.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MessageService extends ServiceImpl<MessageMapper, Message> {

    private static final String CACHE_PREFIX = "chat:messages:";
    private static final long CACHE_EXPIRE_MINUTES = 10;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public Message saveMessage(String sessionId, String content, Integer senderType, Long senderId) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setContent(content);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setCreateTime(LocalDateTime.now());
        save(message);
        
        updateCache(sessionId);
        
        return message;
    }

    @SuppressWarnings("unchecked")
    public List<Message> getMessagesBySessionId(String sessionId) {
        String cacheKey = CACHE_PREFIX + sessionId;
        
        List<Message> messages = (List<Message>) redisTemplate.opsForValue().get(cacheKey);
        
        if (messages != null) {
            return messages;
        }
        
        messages = list(new LambdaQueryWrapper<Message>()
                .eq(Message::getSessionId, sessionId)
                .orderByAsc(Message::getCreateTime));
        
        if (messages != null && !messages.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, messages, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }
        
        return messages;
    }

    public Page<Message> getMessagePage(String sessionId, Integer pageNum, Integer pageSize) {
        Page<Message> page = new Page<>(pageNum, pageSize);
        return page(page, new LambdaQueryWrapper<Message>()
                .eq(Message::getSessionId, sessionId)
                .orderByDesc(Message::getCreateTime));
    }

    public void deleteBySessionId(String sessionId) {
        remove(new LambdaQueryWrapper<Message>()
                .eq(Message::getSessionId, sessionId));
        
        redisTemplate.delete(CACHE_PREFIX + sessionId);
    }
    
    private void updateCache(String sessionId) {
        String cacheKey = CACHE_PREFIX + sessionId;
        
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            List<Message> messages = list(new LambdaQueryWrapper<Message>()
                    .eq(Message::getSessionId, sessionId)
                    .orderByAsc(Message::getCreateTime));
            
            if (messages != null && !messages.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, messages, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            }
        }
    }
}