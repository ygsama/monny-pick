package com.example.demo.service;

import com.example.demo.dao.KLineData;
import lombok.Getter;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 股票分析提示词数据对象
 */
@Getter
@Setter
public class StockAnalysisPrompt {
    private String stockCode;
    private Date analysisDate;
    private List<KLineData> historicalData;
    private String prompt;
    private Boolean actualNextPeriodRise; // 实际下一周期涨跌情况
    private Integer klt; // K线周期
    private String modelPrediction;    // 模型预测结果
    private Double confidence;         // 模型置信度
    private String reasoning;          // 模型分析理由

    public StockAnalysisPrompt(String stockCode, Date analysisDate,
                               List<KLineData> historicalData, String prompt,
                               Boolean actualNextPeriodRise) {
        this(stockCode, analysisDate, historicalData, prompt, actualNextPeriodRise, 101);
    }

    public StockAnalysisPrompt(String stockCode, Date analysisDate,
                               List<KLineData> historicalData, String prompt,
                               Boolean actualNextPeriodRise, Integer klt) {
        this.stockCode = stockCode;
        this.analysisDate = analysisDate;
        this.historicalData = historicalData;
        this.prompt = prompt;
        this.actualNextPeriodRise = actualNextPeriodRise;
        this.klt = klt;
    }

    /**
     * 检查预测是否正确
     */
    public boolean isPredictionCorrect() {
        if (modelPrediction == null || actualNextPeriodRise == null) {
            return false;
        }
        boolean predictedRise = "上涨".equals(modelPrediction);
        return predictedRise == actualNextPeriodRise;
    }

    /**
     * 获取周期名称
     */
    public String getPeriodName() {
        switch (klt) {
            case 101: return "日K";
            case 102: return "周K";
            case 103: return "月K";
            case 104: return "季K";
            case 105: return "半年K";
            case 106: return "年K";
            default: return "未知";
        }
    }

    @Override
    public String toString() {
        return "StockAnalysisPrompt{\n" +
                "stockCode='" + stockCode + "'\n" +
                "analysisDate=" + new SimpleDateFormat("yyyy-MM-dd").format(analysisDate) + "\n" +
                "period=" + getPeriodName() + "\n" +
                "historicalDataCount=" + (historicalData != null ? historicalData.size() : 0) + "\n" +
                "actualNextPeriodRise=" + actualNextPeriodRise + "\n" +
                "modelPrediction='" + modelPrediction + "'\n" +
                "confidence=" + confidence + "\n" +
                "isCorrect=" + isPredictionCorrect() + "\n" +
                "}\n" +
                "Prompt Preview:\n" +
                (prompt != null ? prompt.substring(0, Math.min(200, prompt.length())) + "..." : "null");
    }
}
