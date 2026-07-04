package com.example.facedemo.controller;

import com.example.facedemo.model.FaceResult;
import com.example.facedemo.service.FaceDetectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket处理器 - 处理人脸检测请求
 */
@Component
public class FaceDetectionController extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(FaceDetectionController.class);

    // 最大消息大小限制 (2MB)
    private static final int MAX_MESSAGE_SIZE = 2 * 1024 * 1024;

    private final FaceDetectionService faceDetectionService;
    private final ObjectMapper objectMapper;

    public FaceDetectionController(FaceDetectionService faceDetectionService, ObjectMapper objectMapper) {
        this.faceDetectionService = faceDetectionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket连接已建立: {}", session.getId());
        // 设置大消息缓冲区
        session.setTextMessageSizeLimit(MAX_MESSAGE_SIZE);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 检查消息大小
        if (payload.length() > MAX_MESSAGE_SIZE) {
            logger.warn("消息太大: {} 字符, 限制: {} 字符", payload.length(), MAX_MESSAGE_SIZE);
            session.close(new CloseStatus(1009, "Message too large"));
            return;
        }

        logger.debug("收到图像数据，长度: {} 字符", payload.length());

        try {
            // 解析前端发送的Base64图像数据
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> request = objectMapper.readValue(payload, java.util.Map.class);
            String imageData = (String) request.get("image");

            if (imageData == null || imageData.isEmpty()) {
                logger.warn("未收到有效的图像数据");
                return;
            }

            // 执行人脸检测
            FaceResult result = faceDetectionService.detect(imageData);

            // 将结果返回给前端
            String jsonResponse = objectMapper.writeValueAsString(result);
            session.sendMessage(new TextMessage(jsonResponse));

        } catch (Exception e) {
            logger.error("人脸检测处理失败", e);
            try {
                FaceResult errorResult = new FaceResult(0, java.util.Collections.emptyList());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResult)));
            } catch (Exception sendError) {
                logger.error("发送错误响应失败", sendError);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("WebSocket连接已关闭: {}, 状态: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误", exception);
    }
}
