package com.example.ai.controller;

import com.example.ai.dto.ChatRequest;
import com.example.ai.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private DeepSeekService deepSeekService;

    @PostMapping("/chat/rag")
    public Map<String, Object> chatWithRAGAndFunctionCalling(@RequestBody Map<String, Object> params) {
        String message = (String) params.get("message");

        @SuppressWarnings("unchecked")
        List<Map<String, String>> historyMaps = (List<Map<String, String>>) params.get("history");
        List<ChatRequest.Message> history = null;

        if (historyMaps != null) {
            history = historyMaps.stream()
                    .map(m -> new ChatRequest.Message(m.get("role"), m.get("content")))
                    .toList();
        }

        Map<String, Object> aiResult = deepSeekService.chatWithRAGAndFunctionCalling(message, history);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", aiResult);
        return result;
    }

}
