package com.example.facedemo.config;

import com.example.facedemo.controller.FaceDetectionController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final FaceDetectionController faceDetectionController;

    public WebSocketConfig(FaceDetectionController faceDetectionController) {
        this.faceDetectionController = faceDetectionController;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(faceDetectionController, "/ws/face")
                .setAllowedOriginPatterns("*");
    }
}
