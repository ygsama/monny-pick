package com.example.demo.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 分析统计结果
 */
public class AnalysisStatistics {
    private int totalTests;          // 总测试次数
    private int correctPredictions;  // 正确预测次数
    private double accuracy;         // 准确率
    private String message;          // 统计信息
    private List<TestResult> testResults; // 详细测试结果

    public AnalysisStatistics() {
        this.testResults = new ArrayList<>();
    }

    // 添加测试结果
    public void addTestResult(Date testDate, boolean predicted, boolean actual,
                              double confidence, boolean isCorrect) {
        testResults.add(new TestResult(testDate, predicted, actual, confidence, isCorrect));
    }

    // Getter和Setter
    public int getTotalTests() { return totalTests; }
    public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

    public int getCorrectPredictions() { return correctPredictions; }
    public void setCorrectPredictions(int correctPredictions) { this.correctPredictions = correctPredictions; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<TestResult> getTestResults() { return testResults; }

    /**
     * 单次测试结果
     */
    public static class TestResult {
        private Date testDate;
        private boolean predictedRise;
        private boolean actualRise;
        private double confidence;
        private boolean correct;

        public TestResult(Date testDate, boolean predictedRise, boolean actualRise,
                          double confidence, boolean correct) {
            this.testDate = testDate;
            this.predictedRise = predictedRise;
            this.actualRise = actualRise;
            this.confidence = confidence;
            this.correct = correct;
        }

        // Getter方法
        public Date getTestDate() { return testDate; }
        public boolean isPredictedRise() { return predictedRise; }
        public boolean isActualRise() { return actualRise; }
        public double getConfidence() { return confidence; }
        public boolean isCorrect() { return correct; }
    }
}