package com.example.ai.controller;

import com.example.ai.entity.Product;
import com.example.ai.service.DeepSeekService;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private static final String INDEX_NAME = "phone_products";

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private DeepSeekService deepSeekService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search( 
        @RequestParam("query") String query,   // 必须加！
        @RequestParam(value = "topK", defaultValue = "5") int topK) {
        try {
            List<Product> products = deepSeekService.searchProducts(query, topK);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", products);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

}