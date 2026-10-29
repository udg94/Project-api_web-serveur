package com.example.projectapi.websocket;

import com.example.projectapi.websocket.MessageHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MessageHandler messageHandler;
    private final HandshakeInterceptor jwtInterceptor;

    public WebSocketConfig(MessageHandler messageHandler, JwtHandshakeInterceptor jwtInterceptor) {
        this.messageHandler = messageHandler;
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(messageHandler, "/ws/messages").addInterceptors(jwtInterceptor).setAllowedOrigins("*");
    }
}