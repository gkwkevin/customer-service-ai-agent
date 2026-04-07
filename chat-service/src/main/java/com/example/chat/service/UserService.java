package com.example.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chat.entity.User;
import com.example.chat.mapper.UserMapper;
import com.example.chat.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public User register(String username, String password, String nickname) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setNickname(nickname != null ? nickname : username);
        user.setRole(1);
        user.setStatus(1);
        userMapper.insert(user);
        
        user.setPassword(null);
        return user;
    }
    
    public String login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (!user.getPassword().equals(encryptPassword(password))) {
            throw new RuntimeException("密码错误");
        }
        
        if (user.getStatus() == 2) {
            throw new RuntimeException("账号已被禁用");
        }
        
        return jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
    }
    
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
    
    public List<User> getAllUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(User.class, info -> !"password".equals(info.getColumn()));
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectList(wrapper);
    }
    
    public User updateUserRole(Long userId, Integer role) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setRole(role);
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }
    
    public User updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }
    
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
