package com.example.chat.common;

public class UserRole {
    public static final int USER = 1;
    public static final int AGENT = 2;
    public static final int ADMIN = 3;
    
    public static String getRoleName(Integer role) {
        if (role == null) return "未知";
        switch (role) {
            case USER: return "普通用户";
            case AGENT: return "客服";
            case ADMIN: return "管理员";
            default: return "未知";
        }
    }
    
    public static boolean isValidRole(Integer role) {
        return role != null && role >= 1 && role <= 3;
    }
}
