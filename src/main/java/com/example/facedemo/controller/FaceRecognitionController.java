package com.example.facedemo.controller;

import com.example.facedemo.service.FaceRecognitionService;
import com.example.facedemo.service.FaceRecognitionService.RecognitionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 人脸识别 REST 接口
 *
 * POST /api/face/register  — 注册人脸
 * POST /api/face/recognize — 识别人脸
 */
@RestController
@RequestMapping("/api/face")
public class FaceRecognitionController {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionController.class);

    private final FaceRecognitionService recognitionService;

    public FaceRecognitionController(FaceRecognitionService recognitionService) {
        this.recognitionService = recognitionService;
    }

    /**
     * 注册人脸
     * 请求体：{ "name": "张三", "image": "data:image/jpeg;base64,..." }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String image = body.get("image");

        Map<String, Object> resp = new HashMap<>();

        if (name == null || name.trim().isEmpty()) {
            resp.put("success", false);
            resp.put("message", "姓名不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        if (image == null || image.isEmpty()) {
            resp.put("success", false);
            resp.put("message", "图像数据不能为空");
            return ResponseEntity.badRequest().body(resp);
        }

        try {
            String msg = recognitionService.register(name.trim(), image);
            resp.put("success", true);
            resp.put("message", msg);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("注册失败: {}", e.getMessage());
            resp.put("success", false);
            resp.put("message", "注册失败：" + e.getMessage());
            return ResponseEntity.ok(resp);
        }
    }

    /**
     * 识别人脸
     * 请求体：{ "image": "data:image/jpeg;base64,..." }
     */
    @PostMapping("/recognize")
    public ResponseEntity<Map<String, Object>> recognize(@RequestBody Map<String, String> body) {
        String image = body.get("image");
        Map<String, Object> resp = new HashMap<>();

        if (image == null || image.isEmpty()) {
            resp.put("success", false);
            resp.put("message", "图像数据不能为空");
            return ResponseEntity.badRequest().body(resp);
        }

        try {
            RecognitionResult result = recognitionService.recognize(image);
            resp.put("success", true);
            resp.put("matched", result.isMatched());
            resp.put("name", result.getName());
            resp.put("similarity", result.getSimilarity());
            resp.put("message", result.getMessage());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.error("识别失败: {}", e.getMessage());
            resp.put("success", false);
            resp.put("matched", false);
            resp.put("message", "识别异常：" + e.getMessage());
            return ResponseEntity.ok(resp);
        }
    }
}
