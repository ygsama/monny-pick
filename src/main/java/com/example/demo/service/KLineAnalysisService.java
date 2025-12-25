package com.example.demo.service;

import com.example.demo.dao.KLineDao;
import com.example.demo.dao.KLineData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * K线分析服务层
 */
@Service
public class KLineAnalysisService {

    @Autowired
    private KLineDao kLineDao;

    private static final double T1_WEIGHT = 0.8;    // T-1权重
    private static final double T2_TO_T5_WEIGHT = 0.2; // T-2到T-5总权重
    private static final double SINGLE_DAY_WEIGHT = T2_TO_T5_WEIGHT / 4; // T-2到T-5单日权重

    /**
     * 分析指定日期的K线数据，预测次日涨跌
     */
    public TrendAnalysisResult analyzeNextDayTrend(String stockCode, Date targetDate) {
        try {
            // 1. 检查目标日期数据是否存在
            KLineData targetData = kLineDao.getKLineDataByDate(stockCode, targetDate);
            if (targetData == null) {
                return new TrendAnalysisResult(false, false, 0,
                        "目标日期数据不存在", "无法获取" + new SimpleDateFormat("yyyy-MM-dd").format(targetDate) + "的数据");
            }

            // 2. 获取T-1到T-5的数据
            List<DailyAnalysis> dailyAnalyses = getHistoricalAnalyses(stockCode, targetDate);
            if (dailyAnalyses.isEmpty()) {
                return new TrendAnalysisResult(false, false, 0,
                        "历史数据不足，无法进行分析", "需要至少T-1的数据进行分析");
            }

            // 3. 计算加权信号
            TrendAnalysisResult result = calculateWeightedSignal(dailyAnalyses, targetDate);
            result.setAnalysisDetails(generateAnalysisDetails(dailyAnalyses, result));

            return result;

        } catch (Exception e) {
            return new TrendAnalysisResult(false, false, 0,
                    "分析过程中发生错误: " + e.getMessage(), "错误详情: " + e.toString());
        }
    }

    /**
     * 获取T-1到T-5的历史数据分析结果
     */
    private List<DailyAnalysis> getHistoricalAnalyses(String stockCode, Date targetDate) {
        List<DailyAnalysis> analyses = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();



        KLineData targetDayData = kLineDao.getKLineDataByDate(stockCode, targetDate);
        if (targetDayData == null) {
            return analyses;
        }

        for (int i = 1; i <= 7; i++) {
            // 计算T-n日期
            calendar.setTime(targetDate);
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            Date nextDate = calendar.getTime();

            // 获取T-n和T-(n+1)的数据
            KLineData nextDayData = kLineDao.getKLineDataByDate(stockCode, nextDate);
            if (nextDayData == null) continue;

            // 分析单日数据
            DailyAnalysis analysis = analyzeSingleDay(i, targetDayData, nextDayData);
            if (analysis != null) {
                analyses.add(analysis);
            }
            if (analyses.size() >= 5) {
                break;
            }
        }
        return analyses;
    }

    /**
     * 分析单日数据
     */
    private DailyAnalysis analyzeSingleDay(int daysBefore, KLineData currentDay, KLineData nextDay) {
        try {
            // 检查数据完整性
            if (currentDay.getVolume() == null || nextDay.getVolume() == null ||
                    currentDay.getClose() == null || currentDay.getOpen() == null ||
                    nextDay.getClose() == null || nextDay.getOpen() == null) {
                return null;
            }

            // 判断涨跌
            boolean isRise = currentDay.getClose() >= currentDay.getOpen();

            // 判断量能变化（缩量还是放量）
            boolean isVolumeShrink = currentDay.getVolume() < nextDay.getVolume();

            // 计算信号强度和方向
            boolean signalRise;
            double signalStrength = calculateSignalStrength(currentDay, nextDay);

            // 根据规则判断信号方向
            if (isVolumeShrink) {
                // 缩量涨->继续涨，缩量跌->继续跌
                signalRise = isRise;
            } else {
                // 放量涨->反转跌，放量跌->反转涨
                signalRise = !isRise;
            }

            // 设置权重
            double weight = (daysBefore == 1) ? T1_WEIGHT : SINGLE_DAY_WEIGHT;

            return new DailyAnalysis(daysBefore, isRise, isVolumeShrink, weight, signalRise, signalStrength);

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算信号强度（基于涨跌幅和量能变化程度）
     */
    private double calculateSignalStrength(KLineData currentDay, KLineData nextDay) {
        // 计算涨跌幅 昨收价格做分母
        double currentChange = Math.abs((currentDay.getClose() - currentDay.getLastClose()) / currentDay.getLastClose());

        // 计算量能变化率
        double volumeChangeRatio = Math.abs((double)(currentDay.getVolume() - nextDay.getVolume()) / nextDay.getVolume());

        // 综合信号强度（涨跌幅和量能变化的加权平均）
//        return (currentChange * 0.7 + volumeChangeRatio * 0.3);
        return 1;
    }

    /**
     * 计算加权信号
     */
    private TrendAnalysisResult calculateWeightedSignal(List<DailyAnalysis> analyses, Date targetDate) {
        double weightedSum = 0;
        double totalWeight = 0;

        for (DailyAnalysis analysis : analyses) {
            double signalValue = analysis.isSignalRise() ? analysis.getSignalStrength() : -analysis.getSignalStrength();
            weightedSum += signalValue * analysis.getWeight();
            totalWeight += analysis.getWeight();
        }

        // 归一化置信度 todo 原始权重，不看量
        double confidence = (totalWeight > 0) ? weightedSum / totalWeight : 0;
        boolean predictedRise = confidence > 0;

        String message = String.format("分析日期: %s, 预测次日: %s, 置信度: %.2f%%",
                new SimpleDateFormat("yyyy-MM-dd").format(targetDate),
                predictedRise ? "上涨" : "下跌",
                Math.abs(confidence) * 100);

        return new TrendAnalysisResult(true, predictedRise, confidence, message, "");
    }

    /**
     * 生成详细分析报告
     */
    private String generateAnalysisDetails(List<DailyAnalysis> analyses, TrendAnalysisResult result) {
        StringBuilder details = new StringBuilder();
        details.append("=== 详细分析报告 ===\n");

        details.append(String.format("\n综合预测: %s\n置信度: %.2f%% \n ",
                result.isPredictedRise() ? "上涨" : "下跌",
                Math.abs(result.getConfidence()) * 100));

        for (DailyAnalysis analysis : analyses) {
            details.append(String.format("T-%d: %s, 量能: %s, 信号: %s, 权重: %.1f%%, 强度: %.2f\n ",
                    analysis.getDaysBefore(),
                    analysis.isRise() ? "涨" : "跌",
                    analysis.isVolumeShrink() ? "缩量" : "放量",
                    analysis.isSignalRise() ? "看涨" : "看跌",
                    analysis.getWeight() * 100,
                    analysis.getSignalStrength()));
        }

        return details.toString();
    }

    /**
     * 批量分析测试方法，过去多少天
     */
    public AnalysisStatistics batchAnalysisTest(String stockCode, int testCount) {
        AnalysisStatistics stats = new AnalysisStatistics();

        // 获取所有有数据的日期
        List<KLineData> allData = kLineDao.getAllKLineData(stockCode);
        if (allData == null || allData.size() <= 6) {
            stats.setMessage("数据量不足，至少需要7天数据");
            return stats;
        }

        // 选择测试日期（排除最后一天，因为需要后一天的真实数据验证）
        int correctPredictions = 0;
        int totalTests = testCount;

        for (int i = 1; i < testCount; i++) {
            // 选择测试日期（可以确保有T-1到T-5的数据）
            Date testDate = allData.get(i).getDate();

            // 获取真实的下一天数据
            Date nextDay = getNextTradeDay(allData, testDate);
            if (nextDay == null) continue;

            KLineData nextDayData = kLineDao.getKLineDataByDate(stockCode, nextDay);
            if (nextDayData == null) continue;

            // 进行分析预测
            TrendAnalysisResult prediction = analyzeNextDayTrend(stockCode, testDate);
            if (!prediction.isCanAnalyze()) continue;

            // 验证预测准确性
            boolean actualRise = nextDayData.getChangeRate() > 0;
            boolean isCorrect = (prediction.isPredictedRise() == actualRise);

            if (isCorrect) correctPredictions++;

            stats.addTestResult(testDate, prediction.isPredictedRise(), actualRise,
                    prediction.getConfidence(), isCorrect);
        }

        // 计算统计结果
        double accuracy = (totalTests > 0) ? (double) correctPredictions / totalTests * 100 : 0;
        stats.setTotalTests(totalTests);
        stats.setCorrectPredictions(correctPredictions);
        stats.setAccuracy(accuracy);
        stats.setMessage(String.format("测试完成: 总数%d, 正确%d, 准确率%.2f%%",
                totalTests, correctPredictions, accuracy));

        return stats;
    }

    /**
     * 获取下一个交易日
     */
    private Date getNextTradeDay(List<KLineData> allData, Date currentDate) {
        // 按日期排序
        allData.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        for (int i = 0; i < allData.size() - 1; i++) {
            if (allData.get(i).getDate().equals(currentDate)) {
                return allData.get(i + 1).getDate();
            }
        }
        return null;
    }
}