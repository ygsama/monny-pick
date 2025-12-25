package com.example.demo.service;

import com.example.demo.dao.KLineData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 股票分析提示词数据对象
 */
public class StockAnalysisPrompt {
    private String stockCode;
    private Date analysisDate;
    private List<KLineData> historicalData;
    private String prompt;
    private Boolean actualNextDayRise; // 实际次日涨跌情况（用于验证）
    private String modelPrediction;    // 模型预测结果
    private Double confidence;         // 模型置信度
    private String reasoning;          // 模型分析理由

    public StockAnalysisPrompt(String stockCode, Date analysisDate,
                               List<KLineData> historicalData, String prompt,
                               Boolean actualNextDayRise) {
        this.stockCode = stockCode;
        this.analysisDate = analysisDate;
        this.historicalData = historicalData;
        this.prompt = prompt;
        this.actualNextDayRise = actualNextDayRise;
    }

    // Getter和Setter方法
    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }

    public Date getAnalysisDate() { return analysisDate; }
    public void setAnalysisDate(Date analysisDate) { this.analysisDate = analysisDate; }

    public List<KLineData> getHistoricalData() { return historicalData; }
    public void setHistoricalData(List<KLineData> historicalData) { this.historicalData = historicalData; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public Boolean getActualNextDayRise() { return actualNextDayRise; }
    public void setActualNextDayRise(Boolean actualNextDayRise) { this.actualNextDayRise = actualNextDayRise; }

    public String getModelPrediction() { return modelPrediction; }
    public void setModelPrediction(String modelPrediction) { this.modelPrediction = modelPrediction; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    /**
     * 检查预测是否正确
     */
    public boolean isPredictionCorrect() {
        if (modelPrediction == null || actualNextDayRise == null) {
            return false;
        }
        boolean predictedRise = "上涨".equals(modelPrediction);
        return predictedRise == actualNextDayRise;
    }

    @Override
    public String toString() {
        return "StockAnalysisPrompt{\n" +
                "stockCode='" + stockCode + "'\n" +
                "analysisDate=" + new SimpleDateFormat("yyyy-MM-dd").format(analysisDate) + "\n" +
                "historicalDataCount=" + (historicalData != null ? historicalData.size() : 0) + "\n" +
                "actualNextDayRise=" + actualNextDayRise + "\n" +
                "modelPrediction='" + modelPrediction + "'\n" +
                "confidence=" + confidence + "\n" +
                "isCorrect=" + isPredictionCorrect() + "\n" +
                "}\n" +
                "Prompt Preview:\n" +
                (prompt != null ? prompt.substring(0, Math.min(200, prompt.length())) + "..." : "null");
    }
}
