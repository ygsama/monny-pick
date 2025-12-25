package com.example.demo.service;

import com.alibaba.fastjson.JSON;

/**
 * 涨跌分析结果
 */
public class TrendAnalysisResult {
    private boolean canAnalyze;          // 是否可分析
    private boolean predictedRise;      // 预测次日是否上涨
    private double confidence;           // 置信度 (-1 到 1)
    private String message;             // 分析结果描述
    private String analysisDetails;      // 详细分析过程

    // 构造方法
    public TrendAnalysisResult() {}

    public TrendAnalysisResult(boolean canAnalyze, boolean predictedRise,
                               double confidence, String message, String analysisDetails) {
        this.canAnalyze = canAnalyze;
        this.predictedRise = predictedRise;
        this.confidence = confidence;
        this.message = message;
        this.analysisDetails = analysisDetails;
    }

    // Getter和Setter方法
    public boolean isCanAnalyze() { return canAnalyze; }
    public void setCanAnalyze(boolean canAnalyze) { this.canAnalyze = canAnalyze; }

    public boolean isPredictedRise() { return predictedRise; }
    public void setPredictedRise(boolean predictedRise) { this.predictedRise = predictedRise; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAnalysisDetails() { return analysisDetails; }
    public void setAnalysisDetails(String analysisDetails) { this.analysisDetails = analysisDetails; }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}