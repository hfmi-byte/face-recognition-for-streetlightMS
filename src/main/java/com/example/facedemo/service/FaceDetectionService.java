package com.example.facedemo.service;

import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionRectangle;
import cn.smartjavaai.common.entity.Point;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.entity.DetectionResponse;
import cn.smartjavaai.common.entity.face.FaceInfo;
import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.face.config.FaceDetConfig;
import cn.smartjavaai.face.enums.FaceDetModelEnum;
import cn.smartjavaai.face.factory.FaceDetModelFactory;
import cn.smartjavaai.face.model.facedect.FaceDetModel;
import com.example.facedemo.model.FaceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 人脸识别服务
 */
@Service
public class FaceDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FaceDetectionService.class);

    @Value("${face.model.path}")
    private String modelPath;

    @Value("${face.model.confidence-threshold:0.3}")
    private float confidenceThreshold;

    @Value("${face.model.nms-threshold:0.4}")
    private float nmsThreshold;

    private FaceDetModel faceModel;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            // 设置OpenCV引擎
            SmartImageFactory.setEngine(SmartImageFactory.Engine.OPENCV);

            FaceDetConfig config = new FaceDetConfig();
            config.setModelEnum(FaceDetModelEnum.MTCNN);
            config.setModelPath(modelPath);
            // 降低置信度阈值以提高召回率
            config.setConfidenceThreshold(Math.max(0.1f, confidenceThreshold));
            // 调整NMS阈值以更好处理重叠人脸
            config.setNmsThresh(nmsThreshold);

            faceModel = FaceDetModelFactory.getInstance().getModel(config);
            initialized = true;
            logger.info("人脸检测服务初始化成功，模型路径: {}, 置信度阈值: {}, NMS阈值: {}",
                modelPath, confidenceThreshold, nmsThreshold);
        } catch (Exception e) {
            logger.error("人脸检测服务初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 检测Base64图像中的人脸
     *
     * @param base64Image Base64编码的图像数据
     * @return 人脸检测结果
     */
    public FaceResult detect(String base64Image) {
        if (!initialized || faceModel == null) {
            logger.warn("人脸检测服务未初始化");
            return new FaceResult(0, new ArrayList<>());
        }

        try {
            // 去除Base64前缀（如 "data:image/jpeg;base64,"）
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.substring(base64Image.indexOf(",") + 1);
            }

            // Base64解码为BufferedImage
            BufferedImage image = decodeToImage(imageData);
            if (image == null) {
                logger.error("图像解码失败");
                return new FaceResult(0, new ArrayList<>());
            }

            // 创建SmartImage并执行人脸检测
            ai.djl.modality.cv.Image smartImage = SmartImageFactory.getInstance().fromBufferedImage(image);
            R<DetectionResponse> result = faceModel.detect(smartImage);

            if (result.isSuccess() && result.getData() != null) {
                List<FaceResult.FaceInfo> faceInfoList = new ArrayList<>();

                for (DetectionInfo detectionInfo : result.getData().getDetectionInfoList()) {
                    DetectionRectangle rectangle = detectionInfo.getDetectionRectangle();

                    // 提取人脸关键点
                    List<FaceResult.Point> landmarks = extractLandmarks(detectionInfo);

                    FaceResult.FaceInfo faceInfo = new FaceResult.FaceInfo(
                        rectangle.getX(),
                        rectangle.getY(),
                        rectangle.getWidth(),
                        rectangle.getHeight(),
                        detectionInfo.getScore(),
                        landmarks
                    );
                    faceInfoList.add(faceInfo);
                }

                logger.debug("检测到 {} 张人脸", faceInfoList.size());
                return new FaceResult(faceInfoList.size(), faceInfoList);
            } else {
                logger.error("人脸检测失败: {}", result.getMessage());
                return new FaceResult(0, new ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("人脸检测异常", e);
            return new FaceResult(0, new ArrayList<>());
        }
    }

    /**
     * 从DetectionInfo中提取5点人脸关键点
     * 关键点顺序: 左眼, 右眼, 鼻子, 左嘴角, 右嘴角
     */
    private List<FaceResult.Point> extractLandmarks(DetectionInfo detectionInfo) {
        List<FaceResult.Point> landmarks = new ArrayList<>();

        try {
            FaceInfo faceInfo = detectionInfo.getFaceInfo();
            if (faceInfo != null && faceInfo.getKeyPoints() != null) {
                List<Point> keyPoints = faceInfo.getKeyPoints();
                for (Point kp : keyPoints) {
                    landmarks.add(new FaceResult.Point((float) kp.getX(), (float) kp.getY()));
                }
            }
        } catch (Exception e) {
            logger.debug("提取关键点失败: {}", e.getMessage());
        }

        return landmarks;
    }

    /**
     * Base64字符串解码为BufferedImage
     */
    private BufferedImage decodeToImage(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            return ImageIO.read(bis);
        } catch (IOException e) {
            logger.error("图像解码失败", e);
            return null;
        }
    }
}
