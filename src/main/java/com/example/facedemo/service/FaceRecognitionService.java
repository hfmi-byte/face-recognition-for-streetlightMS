package com.example.facedemo.service;

import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.face.config.FaceDetConfig;
import cn.smartjavaai.face.config.FaceRecConfig;
import cn.smartjavaai.face.enums.FaceDetModelEnum;
import cn.smartjavaai.face.enums.FaceRecModelEnum;
import cn.smartjavaai.face.factory.FaceDetModelFactory;
import cn.smartjavaai.face.factory.FaceRecModelFactory;
import cn.smartjavaai.face.model.facedect.FaceDetModel;
import cn.smartjavaai.face.model.facerec.FaceRecModel;
import cn.smartjavaai.common.entity.R;
import com.example.facedemo.entity.FaceFeature;
import com.example.facedemo.entity.Person;
import com.example.facedemo.repository.FaceFeatureRepository;
import com.example.facedemo.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;

/**
 * 人脸识别服务（注册 + 比对）
 *
 * 流程：
 *   注册：base64图 → extractTopFaceFeature → float[] → 编码存DB
 *   识别：base64图 → extractTopFaceFeature → float[] → 遍历DB用calculSimilar比对 → 返回最近邻
 */
@Service
public class FaceRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionService.class);

    // ── 配置项 ───────────────────────────────────────────────

    @Value("${face.model.path}")
    private String detModelPath;

    @Value("${face.recognition.model.path}")
    private String recModelPath;

    /**
     * 相似度阈值，超过此值认为是同一人。
     * calculSimilar 返回余弦相似度，典型范围 [-1, 1]，同一人通常在 0.3 以上。
     * 建议根据实际测试调整（application.yml 中的 face.recognition.threshold）。
     */
    @Value("${face.recognition.threshold:0.35}")
    private float similarityThreshold;

    // ── 模型 ─────────────────────────────────────────────────

    private FaceRecModel recModel;
    private boolean initialized = false;

    // ── 数据层 ───────────────────────────────────────────────

    private final PersonRepository personRepo;
    private final FaceFeatureRepository featureRepo;

    public FaceRecognitionService(PersonRepository personRepo,
                                   FaceFeatureRepository featureRepo) {
        this.personRepo = personRepo;
        this.featureRepo = featureRepo;
    }

    // ────────────────────────────────────────────────────────
    // 初始化
    // ────────────────────────────────────────────────────────

    @PostConstruct
    public void init() {
        try {
            SmartImageFactory.setEngine(SmartImageFactory.Engine.OPENCV);

            // 先初始化检测模型（extractTopFaceFeature 内部需要用它定位人脸）
            FaceDetConfig detConfig = new FaceDetConfig();
            detConfig.setModelEnum(FaceDetModelEnum.MTCNN);
            detConfig.setModelPath(detModelPath);
            detConfig.setConfidenceThreshold(0.3f);
            FaceDetModel detModel = FaceDetModelFactory.getInstance().getModel(detConfig);

            // 再初始化识别模型，并注入检测模型
            FaceRecConfig config = new FaceRecConfig();
            config.setModelEnum(FaceRecModelEnum.FACENET_MODEL);  // 对应 face_feature.pt
            config.setModelPath(recModelPath);
            config.setCropFace(true);   // 自动裁剪人脸区域
            config.setAlign(true);      // 自动人脸对齐，提升精度
            config.setDetectModel(detModel);  // 必须：提供检测模型

            recModel = FaceRecModelFactory.getInstance().getModel(config);
            initialized = true;
            logger.info("人脸识别服务初始化成功，模型: {}, 阈值: {}",
                    FaceRecModelEnum.FACENET_MODEL, similarityThreshold);
        } catch (Exception e) {
            logger.error("人脸识别服务初始化失败: {}", e.getMessage(), e);
        }
    }

    // ────────────────────────────────────────────────────────
    // 公共接口
    // ────────────────────────────────────────────────────────

    /**
     * 注册人脸
     *
     * @param name        人员姓名（同名会追加特征，不会重复创建人员）
     * @param base64Image 包含人脸的 base64 图像
     * @return 成功消息
     */
    @Transactional
    public String register(String name, String base64Image) {
        if (!initialized) {
            throw new IllegalStateException("人脸识别服务未初始化，请检查模型路径配置");
        }
        BufferedImage image = decodeBase64(base64Image);
        float[] feature = extractFeature(image);

        // 同名则追加特征，否则新建人员记录
        Person person = personRepo.findByName(name)
                .orElseGet(() -> {
                    Person p = new Person();
                    p.setName(name);
                    return personRepo.save(p);
                });

        FaceFeature ff = new FaceFeature();
        ff.setPerson(person);
        ff.setFeatureVector(encodeFeature(feature));
        featureRepo.save(ff);

        logger.info("注册成功: {}（共 {} 条特征）", name,
                featureRepo.countByPersonId(person.getId()));
        return "注册成功：" + name;
    }

    /**
     * 识别人脸
     *
     * @param base64Image 包含人脸的 base64 图像
     * @return 识别结果
     */
    public RecognitionResult recognize(String base64Image) {
        if (!initialized) {
            return RecognitionResult.unknown("服务未初始化");
        }

        float[] queryFeature;
        try {
            BufferedImage image = decodeBase64(base64Image);
            queryFeature = extractFeature(image);
        } catch (Exception e) {
            logger.warn("特征提取失败: {}", e.getMessage());
            return RecognitionResult.unknown("未检测到人脸");
        }

        List<FaceFeature> allFeatures = featureRepo.findAllWithPerson();
        if (allFeatures.isEmpty()) {
            return RecognitionResult.unknown("数据库中暂无注册人员，请先注册");
        }

        // 遍历全库，找相似度最高的
        float bestScore = Float.NEGATIVE_INFINITY;
        String bestName = null;
        Long bestPersonId = null;

        for (FaceFeature stored : allFeatures) {
            float[] storedVec = decodeFeature(stored.getFeatureVector());
            float score = recModel.calculSimilar(queryFeature, storedVec);
            if (score > bestScore) {
                bestScore = score;
                bestName = stored.getPerson().getName();
                bestPersonId = stored.getPerson().getId();
            }
        }

        logger.debug("识别最高相似度: {} -> {}", bestScore, bestName);

        if (bestScore >= similarityThreshold) {
            return RecognitionResult.matched(bestPersonId, bestName, bestScore);
        } else {
            return RecognitionResult.unknown(
                    String.format("未找到匹配人员（最高相似度: %.3f，阈值: %.2f）",
                            bestScore, similarityThreshold));
        }
    }

    // ────────────────────────────────────────────────────────
    // 内部工具方法
    // ────────────────────────────────────────────────────────

    /** 从 BufferedImage 提取人脸特征向量（自动检测 + 裁剪）*/
    private float[] extractFeature(BufferedImage image) {
        // 转为 DJL Image，使用非 deprecated 的 extractTopFaceFeature(Image) 重载
        ai.djl.modality.cv.Image djlImage =
                SmartImageFactory.getInstance().fromBufferedImage(image);
        R<float[]> result = recModel.extractTopFaceFeature(djlImage);
        if (!result.isSuccess() || result.getData() == null) {
            throw new IllegalArgumentException(
                    "人脸特征提取失败: " + result.getMessage());
        }
        return result.getData();
    }

    /** float[] 序列化为 Base64 字符串（存 DB）*/
    private String encodeFeature(float[] feature) {
        ByteBuffer buf = ByteBuffer.allocate(feature.length * Float.BYTES);
        for (float f : feature) buf.putFloat(f);
        return Base64.getEncoder().encodeToString(buf.array());
    }

    /** Base64 字符串反序列化为 float[]（从 DB 读取）*/
    private float[] decodeFeature(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        float[] feature = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < feature.length; i++) feature[i] = buf.getFloat();
        return feature;
    }

    /** base64图像（含或不含 data URI 前缀）→ BufferedImage */
    private BufferedImage decodeBase64(String base64Image) {
        String data = base64Image.contains(",")
                ? base64Image.substring(base64Image.indexOf(',') + 1)
                : base64Image;
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) throw new IllegalArgumentException("图像解码失败");
            return img;
        } catch (IOException e) {
            throw new RuntimeException("图像解码异常", e);
        }
    }

    // ────────────────────────────────────────────────────────
    // 结果 DTO
    // ────────────────────────────────────────────────────────

    public static class RecognitionResult {
        private final boolean matched;
        private final Long personId;
        private final String name;
        private final float similarity;
        private final String message;

        private RecognitionResult(boolean matched, Long personId,
                                   String name, float similarity, String message) {
            this.matched    = matched;
            this.personId   = personId;
            this.name       = name;
            this.similarity = similarity;
            this.message    = message;
        }

        public static RecognitionResult matched(Long personId, String name, float similarity) {
            return new RecognitionResult(true, personId, name, similarity, null);
        }

        public static RecognitionResult unknown(String message) {
            return new RecognitionResult(false, null, null, 0f, message);
        }

        public boolean isMatched()   { return matched; }
        public Long getPersonId()    { return personId; }
        public String getName()      { return name; }
        public float getSimilarity() { return similarity; }
        public String getMessage()   { return message; }
    }
}
