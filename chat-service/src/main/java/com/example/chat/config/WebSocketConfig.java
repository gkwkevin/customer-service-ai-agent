package com.example.chat.config;

import com.example.chat.handler.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            String query = servletRequest.getServletRequest().getQueryString();
                            System.out.println("WebSocket handshake query: " + query);
                            if (query != null) {
                                for (String param : query.split("&")) {
                                    if (param.startsWith("sessionId=")) {
                                        String sessionId = param.split("=")[1];
                                        attributes.put("sessionId", sessionId);
                                        System.out.println("Set sessionId in attributes: " + sessionId);
                                    }
                                    if (param.startsWith("agentId=")) {
                                        try {
                                            Long agentId = Long.parseLong(param.split("=")[1]);
                                            attributes.put("agentId", agentId);
                                            System.out.println("Set agentId in attributes: " + agentId);
                                        } catch (NumberFormatException e) {
                                            System.err.println("Invalid agentId: " + param);
                                        }
                                    }
                                }
                            }
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
                    }
                });
    }
}
