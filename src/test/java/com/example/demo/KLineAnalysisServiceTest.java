package com.example.demo;

import com.example.demo.service.AnalysisStatistics;
import com.example.demo.service.KLineAnalysisService;
import com.example.demo.service.TrendAnalysisResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;

/**
 * K线分析服务测试类
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class KLineAnalysisServiceTest {

    @Autowired
    private KLineAnalysisService kLineAnalysisService;

    @Test
    public void testSingleDateAnalysis() {
        String stockCode = "510300";

        // 测试特定日期
        TrendAnalysisResult result = kLineAnalysisService.analyzeNextDayTrend(stockCode,
                java.sql.Date.valueOf("2025-12-19"));

        System.out.println("单日分析结果:");
        System.out.println(result);
    }

    @Test
    public void testBatchAnalysis() {
        String stockCode = "510300";

        // 批量测试100次
        AnalysisStatistics stats = kLineAnalysisService.batchAnalysisTest(stockCode, 200);

        System.out.println("批量测试结果:");
        System.out.println("总测试次数: " + stats.getTotalTests());
        System.out.println("正确预测次数: " + stats.getCorrectPredictions());
        System.out.println("准确率: " + String.format("%.2f%%", stats.getAccuracy()));
        System.out.println("详细信息: " + stats.getMessage());

        // 打印前10条详细结果
        System.out.println("\n前100条详细结果:");
        for (int i = 0; i < Math.min(100, stats.getTestResults().size()); i++) {
            AnalysisStatistics.TestResult testResult = stats.getTestResults().get(i);
            System.out.printf("日期: %s, 预测: %s, 实际: %s, 正确: %s, 置信度: %.2f%%\n",
                    new SimpleDateFormat("yyyy-MM-dd").format(testResult.getTestDate()),
                    testResult.isPredictedRise() ? "涨" : "跌",
                    testResult.isActualRise() ? "涨" : "跌",
                    testResult.isCorrect() ? "是" : "否",
                    testResult.getConfidence() * 100);
        }
    }
}