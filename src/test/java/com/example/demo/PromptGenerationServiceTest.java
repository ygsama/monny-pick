package com.example.demo;

import com.example.demo.service.PromptGenerationService;
import com.example.demo.service.StockAnalysisPrompt;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 提示词生成服务测试类
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class PromptGenerationServiceTest {

    @Autowired
    private PromptGenerationService promptGenerationService;

    @Test
    public void testGeneratePrompts() {
//        String stockCode = "510300";
        String stockCode = "510500";

        // 生成10个随机日期的提示词
        List<StockAnalysisPrompt> prompts =
                promptGenerationService.generateDefaultPrompts(stockCode);

        System.out.println("生成的提示词数量: " + prompts.size());
        System.out.println("==============================================");

        for (int i = 0; i < prompts.size(); i++) {
            StockAnalysisPrompt prompt = prompts.get(i);
            System.out.println("提示词 #" + (i + 1) + ":");
            System.out.println("分析日期: " +
                    new SimpleDateFormat("yyyy-MM-dd").format(prompt.getAnalysisDate()));
            System.out.println("历史数据天数: " +
                    (prompt.getHistoricalData() != null ? prompt.getHistoricalData().size() : 0));
            System.out.println("实际次日涨跌: " +
                    (prompt.getActualNextPeriodRise() != null ?
                            (prompt.getActualNextPeriodRise() ? "上涨" : "下跌") : "未知"));
            System.out.println("提示词预览:");
            System.out.println(prompt.getPrompt().substring(0,
                    Math.min(300, prompt.getPrompt().length())) + "...");
            System.out.println("----------------------------------------------");
        }
    }

    @Test
    public void testSinglePromptGeneration() throws Exception {
        String stockCode = "510500";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date targetDate = sdf.parse("2025-10-19");

        StockAnalysisPrompt prompt =
                promptGenerationService.generateSinglePrompt(stockCode, targetDate,101);

        if (prompt != null) {
            System.out.println("单日提示词生成结果:");
            System.out.println("分析日期: " + sdf.format(prompt.getAnalysisDate()));
            System.out.println("历史数据条数: " + prompt.getHistoricalData().size());
            System.out.println("实际下期涨跌: " +
                    (prompt.getActualNextPeriodRise() ? "上涨" : "下跌"));
            System.out.println("\n完整提示词:");
            System.out.println(prompt.getPrompt());
        } else {
            System.out.println("提示词生成失败，可能数据不足");
        }
    }
}