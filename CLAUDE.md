# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

人脸检测 Demo - 一个基于 Spring Boot 的应用程序，使用 SmartJavaAI 库和 MTCNN 模型通过 WebSocket 提供实时人脸检测功能。

## 构建命令

```bash
# 构建项目
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run

# 运行测试
mvn test
```

## 架构设计

标准的 Spring Boot 分层架构：

```
src/main/java/com/example/facedemo/
├── FaceDetectionApplication.java    # Spring Boot 入口类
├── config/
│   └── WebSocketConfig.java         # WebSocket 端点注册，路径为 /ws/face
├── controller/
│   └── FaceDetectionController.java # WebSocket 处理器 (TextWebSocketHandler)
├── model/
│   └── FaceResult.java              # 数据传输对象，包含 FaceInfo 和 Point 内部类
└── service/
    └── FaceDetectionService.java    # 使用 SmartImageFactory 的人脸检测逻辑
```

**请求流程**：WebSocket 客户端 → `FaceDetectionController` → `FaceDetectionService` → SmartJavaAI MTCNN 模型

## 关键配置

- 模型路径：`/Users/wangweijie/model/mtcnn`（在 `application.yml` 中配置，同时在 `FaceDetectionService.java:57` 硬编码）
- WebSocket 端点：`ws://host:8080/ws/face`
- 服务端口：`8080`

## 依赖说明

- **SmartJavaAI**：人脸检测库 (v1.1.1)
- **OpenCV 4.9.0**：计算机视觉引擎（通过 JavaCV）
- **MTCNN**：人脸检测模型
- **Spring Boot 2.7.18**：Web 和 WebSocket 支持
- **Java 11**：目标版本
