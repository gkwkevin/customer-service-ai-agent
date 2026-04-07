package com.example.chat.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "ai-service", url = "${ai.service.url:http://localhost:8082}")
public interface AiServiceClient {

    @PostMapping("/api/ai/chat")
    Map<String, Object> chat(@RequestBody Map<String, String> request);
}