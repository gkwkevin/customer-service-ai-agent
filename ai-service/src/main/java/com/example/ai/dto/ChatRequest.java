package com.example.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private List<Tool> tools;

    @Data
    public static class Message {
        private String role;
        private String content;
        private String name;
        @JsonProperty("tool_calls")
        private List<Map<String, Object>> toolCalls;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Data
    public static class Tool {
        private String type;
        private Function function;

        public Tool(String type, Function function) {
            this.type = type;
            this.function = function;
        }
    }

    @Data
    public static class Function {
        private String name;
        private String description;
        private Map<String, Object> parameters;

        public Function(String name, String description, Map<String, Object> parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }
    }
}
