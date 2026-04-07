package com.example.chat.controller;

import com.example.chat.annotation.RequireRole;
import com.example.chat.common.Result;
import com.example.chat.common.UserRole;
import com.example.chat.entity.User;
import com.example.chat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRole(UserRole.ADMIN)
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/users")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Result.success(users);
    }
    
    @PutMapping("/user/{id}/role")
    public Result<User> updateUserRole(@PathVariable("id") Long id, @RequestBody Map<String, Integer> params) {
        Integer role = params.get("role");
        if (!UserRole.isValidRole(role)) {
            return Result.error("无效的角色");
        }
        User user = userService.updateUserRole(id, role);
        return Result.success(user);
    }
    
    @PutMapping("/user/{id}/status")
    public Result<User> updateUserStatus(@PathVariable("id") Long id, @RequestBody Map<String, Integer> params) {
        Integer status = params.get("status");
        User user = userService.updateUserStatus(id, status);
        return Result.success(user);
    }
}
