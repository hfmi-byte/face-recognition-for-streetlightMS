package com.example.facedemo.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 人脸特征向量表 — 每个人可注册多张照片，每张对应一条记录
 * featureVector 用 Base64 编码存储序列化后的 float[]，H2/MySQL 均兼容
 */
@Entity
@Table(name = "face_feature")
public class FaceFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属人员 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /**
     * ArcFace 特征向量（Base64 编码的 float[] 二进制序列化）
     * 切换 MySQL 时 columnDefinition 可改为 LONGTEXT
     */
    @Column(name = "feature_vector", nullable = false, columnDefinition = "TEXT")
    private String featureVector;

    /** 注册时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ── getters / setters ──────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Person getPerson() { return person; }
    public void setPerson(Person person) { this.person = person; }

    public String getFeatureVector() { return featureVector; }
    public void setFeatureVector(String featureVector) { this.featureVector = featureVector; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
