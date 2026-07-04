package com.example.facedemo.model;

import java.util.List;

/**
 * 人脸检测结果DTO
 */
public class FaceResult {

    /**
     * 检测到的人脸数量
     */
    private int faceCount;

    /**
     * 人脸列表
     */
    private List<FaceInfo> faces;

    public FaceResult() {
    }

    public FaceResult(int faceCount, List<FaceInfo> faces) {
        this.faceCount = faceCount;
        this.faces = faces;
    }

    public int getFaceCount() {
        return faceCount;
    }

    public void setFaceCount(int faceCount) {
        this.faceCount = faceCount;
    }

    public List<FaceInfo> getFaces() {
        return faces;
    }

    public void setFaces(List<FaceInfo> faces) {
        this.faces = faces;
    }

    /**
     * 单个人脸信息
     */
    public static class FaceInfo {
        /**
         * 人脸区域 x坐标
         */
        private int x;

        /**
         * 人脸区域 y坐标
         */
        private int y;

        /**
         * 人脸区域宽度
         */
        private int width;

        /**
         * 人脸区域高度
         */
        private int height;

        /**
         * 检测置信度
         */
        private float confidence;

        /**
         * 5点人脸关键点 [左眼, 右眼, 鼻子, 左嘴角, 右嘴角]
         */
        private List<Point> landmarks;

        public FaceInfo() {
        }

        public FaceInfo(int x, int y, int width, int height, float confidence) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.confidence = confidence;
        }

        public FaceInfo(int x, int y, int width, int height, float confidence, List<Point> landmarks) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.confidence = confidence;
            this.landmarks = landmarks;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }

        public List<Point> getLandmarks() {
            return landmarks;
        }

        public void setLandmarks(List<Point> landmarks) {
            this.landmarks = landmarks;
        }
    }

    /**
     * 人脸关键点
     */
    public static class Point {
        private float x;
        private float y;

        public Point() {
        }

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }
    }
}
