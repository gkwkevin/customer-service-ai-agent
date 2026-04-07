package com.example.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ticket.entity.User;
import com.example.ticket.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public Map<Long, String> getUserNicknameMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));
    }
    
    public List<User> getAgents() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getRole, 2, 3);
        wrapper.eq(User::getStatus, 1);
        wrapper.select(User::getId, User::getNickname, User::getUsername);
        return userMapper.selectList(wrapper);
    }
}
