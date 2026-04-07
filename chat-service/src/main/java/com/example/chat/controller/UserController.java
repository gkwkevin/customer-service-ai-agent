package com.example.chat.controller;

import com.example.chat.common.Result;
import com.example.chat.entity.User;
import com.example.chat.service.UserService;
import com.example.chat.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String nickname = params.get("nickname");
        
        if (username == null || username.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.error("密码长度至少6位");
        }
        
        try {
            User user = userService.register(username, password, nickname);
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            data.put("token", token);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        
        if (username == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }
        
        try {
            String token = userService.login(username, password);
            User user = userService.getUserById(jwtUtil.getUserId(token));
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            data.put("token", token);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error("未登录");
        }
        
        token = token.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return Result.error("登录已过期");
        }
        
        Long userId = jwtUtil.getUserId(token);
        User user = userService.getUserById(userId);
        return Result.success(user);
    }
}
