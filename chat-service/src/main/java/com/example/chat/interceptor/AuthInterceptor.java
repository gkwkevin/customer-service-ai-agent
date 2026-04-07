package com.example.chat.interceptor;

import com.example.chat.annotation.RequireRole;
import com.example.chat.common.Result;
import com.example.chat.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        
        if (requireRole == null) {
            return true;
        }
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "未登录");
            return false;
        }
        
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            sendError(response, "登录已过期");
            return false;
        }
        
        Integer userRole = jwtUtil.getRole(token);
        int[] allowedRoles = requireRole.value();
        
        for (int allowed : allowedRoles) {
            if (userRole == allowed) {
                return true;
            }
        }
        
        sendError(response, "权限不足");
        return false;
    }
    
    private void sendError(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        Result<Void> result = Result.error(403, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
