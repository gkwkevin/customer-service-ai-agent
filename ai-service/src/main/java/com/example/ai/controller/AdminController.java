package com.example.ai.controller;

import com.example.ai.service.ProductMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ProductMigrationService migrationService;

    @PostMapping("/migrate")
    public Map<String, Object> migrate() {
        Map<String, Object> result = new HashMap<>();
        try {
            migrationService.migrate();
            result.put("success", true);
            result.put("message", "迁移完成");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "迁移失败: " + e.getMessage());
        }
        return result;
    }
}
