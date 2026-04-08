package com.example.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    @Value("${zhipu.api-key}")
    private String apiKey;

    @Value("${zhipu.embedding-model:embedding-3}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/embeddings";

    public List<Double> getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> texts = new ArrayList<>();
        texts.add(text);
        List<List<Double>> embeddings = getEmbeddings(texts);
        return embeddings.isEmpty() ? new ArrayList<>() : embeddings.get(0);
    }

    public List<List<Double>> getEmbeddings(List<String> texts) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", texts);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                ZHIPU_API_URL, 
                entity, 
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");

            List<List<Double>> result = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embedding = item.get("embedding");
                List<Double> vector = new ArrayList<>();
                for (JsonNode val : embedding) {
                    vector.add(val.asDouble());
                }
                result.add(vector);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("调用智谱Embedding失败: " + e.getMessage(), e);
        }
    }
}
