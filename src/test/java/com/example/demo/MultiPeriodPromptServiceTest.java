package com.example.demo;

import com.example.demo.dao.KLineDao;
import com.example.demo.service.PromptGenerationService;
import com.example.demo.service.StockAnalysisPrompt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 多周期提示词生成服务测试类
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class MultiPeriodPromptServiceTest {

    @Autowired
    private PromptGenerationService promptGenerationService;

    @Autowired
    private KLineDao kLineDao;

    @Test
    public void testDailyKPrompts() {
        System.out.println("=== 测试日K提示词生成 ===");
        String stockCode = "510300";

        List<StockAnalysisPrompt> prompts =
                promptGenerationService.generateMultiplePrompts(stockCode, 101, 5);

        printPromptResults(prompts);
    }

    @Test
    public void testWeeklyKPrompts() {
        System.out.println("=== 测试周K提示词生成 ===");
        String stockCode = "510300";

        List<StockAnalysisPrompt> prompts =
                promptGenerationService.generateMultiplePrompts(stockCode, 102, 5);

        printPromptResults(prompts);
    }

    @Test
    public void testMonthlyKPrompts() {
        System.out.println("=== 测试月K提示词生成 ===");
        String stockCode = "510300";

        List<StockAnalysisPrompt> prompts =
                promptGenerationService.generateMultiplePrompts(stockCode, 103, 5);

        printPromptResults(prompts);
    }

    @Test
    public void testCacheStats() {
        System.out.println("=== 测试缓存统计 ===");

        // 先获取一些数据
        String stockCode = "510300";
        promptGenerationService.generateMultiplePrompts(stockCode, 101, 3);
        promptGenerationService.generateMultiplePrompts(stockCode, 102, 3);
        promptGenerationService.generateMultiplePrompts(stockCode, 103, 3);

        // 查看缓存统计
        System.out.println(kLineDao.getCacheStats());
    }

    @Test
    public void testMultiPeriodAnalysis() throws Exception {
        System.out.println("=== 测试多周期综合分析 ===");
        String stockCode = "510300";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 测试不同周期
        int[] periods = {101, 102, 103};
        String[] periodNames = {"日K", "周K", "月K"};

        for (int i = 0; i < periods.length; i++) {
            System.out.println("\n生成" + periodNames[i] + "提示词：");
            List<StockAnalysisPrompt> prompts =
                    promptGenerationService.generateMultiplePrompts(stockCode, periods[i], 2);

            for (StockAnalysisPrompt prompt : prompts) {
                System.out.printf("日期: %s, 周期: %s, 历史数据: %d条, 实际涨跌: %s%n",
                        sdf.format(prompt.getAnalysisDate()),
                        prompt.getPeriodName(),
                        prompt.getHistoricalData().size(),
                        prompt.getActualNextPeriodRise() != null ?
                                (prompt.getActualNextPeriodRise() ? "上涨" : "下跌") : "未知");
            }
        }
    }

    private void printPromptResults(List<StockAnalysisPrompt> prompts) {
        System.out.println("生成的提示词数量: " + prompts.size());

        for (int i = 0; i < prompts.size(); i++) {
            StockAnalysisPrompt prompt = prompts.get(i);
            System.out.println("\n提示词 #" + (i + 1) + ":");
            System.out.println("分析日期: " +
                    new SimpleDateFormat("yyyy-MM-dd").format(prompt.getAnalysisDate()));
            System.out.println("K线周期: " + prompt.getPeriodName());
            System.out.println("历史数据条数: " +
                    (prompt.getHistoricalData() != null ? prompt.getHistoricalData().size() : 0));
            System.out.println("实际下一周期涨跌: " +
                    (prompt.getActualNextPeriodRise() != null ?
                            (prompt.getActualNextPeriodRise() ? "上涨" : "下跌") : "未知"));
            System.out.println("提示词预览:");
            if (prompt.getPrompt() != null) {
                String preview = prompt.getPrompt().substring(0,
                        Math.min(300, prompt.getPrompt().length())) + "...";
                System.out.println(preview);
            }
        }
    }
}