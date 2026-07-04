package com.example.facedemo.repository;

import com.example.facedemo.entity.FaceFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FaceFeatureRepository extends JpaRepository<FaceFeature, Long> {

    /**
     * 查询所有特征向量（识别时需要与全库比对）
     * FETCH JOIN 避免 N+1 查询
     */
    @Query("SELECT f FROM FaceFeature f JOIN FETCH f.person")
    List<FaceFeature> findAllWithPerson();

    /** 统计某人已注册的特征数量 */
    long countByPersonId(Long personId);
}
