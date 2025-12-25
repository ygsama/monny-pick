package com.example.demo.service;

import com.example.demo.dao.KLineDao;
import com.example.demo.dao.KLineData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 提示词生成服务
 */
@Service
public class PromptGenerationService {

    @Autowired
    private KLineDao kLineDao;

    private static final int RANDOM_DAYS_COUNT = 10;
    private static final int HISTORICAL_DAYS = 30;
    private final Random random = new Random();

    /**
     * 生成多个随机日期的分析提示词
     */
    public List<StockAnalysisPrompt> generateMultiplePrompts(String stockCode, int count) {
        List<StockAnalysisPrompt> prompts = new ArrayList<>();

        // 获取所有可用的日期
        List<Date> availableDates = getAvailableDatesWithSufficientHistory(stockCode);
        if (availableDates.size() < count) {
            count = availableDates.size();
        }

        // 随机选择日期
        Collections.shuffle(availableDates);
        List<Date> selectedDates = availableDates.subList(0, Math.min(count, availableDates.size()));

        for (Date targetDate : selectedDates) {
            StockAnalysisPrompt prompt = generateSinglePrompt(stockCode, targetDate);
            if (prompt != null) {
                prompts.add(prompt);
            }
        }

        return prompts;
    }

    /**
     * 生成单个日期的分析提示词
     */
    public StockAnalysisPrompt generateSinglePrompt(String stockCode, Date targetDate) {
        try {
            // 获取目标日期前30天的历史数据
            List<KLineData> historicalData = getHistoricalData(stockCode, targetDate, 90);
            if (historicalData == null || historicalData.size() < HISTORICAL_DAYS) {
                return null;
            }

            // 获取目标日期后一天的实际数据（用于验证）
            KLineData nextDayData = getNextDayData(stockCode, targetDate);
            boolean actualRise = nextDayData != null &&
                    nextDayData.getClose() > nextDayData.getLastClose();

            // 构建提示词
            String prompt = buildAnalysisPrompt(stockCode, targetDate, historicalData);

            return new StockAnalysisPrompt(
                    stockCode, targetDate, historicalData, prompt, actualRise
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取目标日期前N天的历史数据
     */
    private List<KLineData> getHistoricalData(String stockCode, Date targetDate, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(targetDate);
        calendar.add(Calendar.DAY_OF_MONTH, -days);
        Date startDate = calendar.getTime();

        return kLineDao.getKLineData(stockCode, startDate, targetDate);
    }

    /**
     * 获取目标日期后一天的数据
     */
    private KLineData getNextDayData(String stockCode, Date targetDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(targetDate);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date nextDay = calendar.getTime();
        KLineData byDate = kLineDao.getKLineDataByDate(stockCode, nextDay);
        for (int i = 0; i < 5; i++) {
            if (byDate == null){
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                byDate = kLineDao.getKLineDataByDate(stockCode, calendar.getTime());
            }
        }
        return kLineDao.getKLineDataByDate(stockCode, nextDay);
    }

    /**
     * 获取所有有足够历史数据的可用日期
     */
    private List<Date> getAvailableDatesWithSufficientHistory(String stockCode) {
        List<Date> availableDates = new ArrayList<>();

        // 获取所有数据
        List<KLineData> allData = kLineDao.getAllKLineData(stockCode);
        if (allData == null || allData.size() <= HISTORICAL_DAYS) {
            return availableDates;
        }

        // 按日期排序（从早到晚）
        allData.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        // 从第31个数据点开始（确保有前30天数据）
        for (int i = HISTORICAL_DAYS; i < allData.size() - 1; i++) {
            availableDates.add(allData.get(i).getDate());
        }

        return availableDates;
    }

    /**
     * 构建分析提示词
     */
    private String buildAnalysisPrompt(String stockCode, Date targetDate,
                                       List<KLineData> historicalData) {
        StringBuilder prompt = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        prompt.append("你是一个数学专业出身的股票实盘大赛冠军，请分析以下股票历史数据，预测下一个交易日的涨跌情况：\n\n");
        prompt.append("股票代码：").append(stockCode).append("\n");
        prompt.append("分析日期：").append(sdf.format(targetDate)).append("\n");
        prompt.append("历史数据（最近几十个交易日）：\n");

        // 添加表头
        prompt.append(String.format("%-12s %-8s %-8s %-8s %-8s %-8s %-12s %-12s %-6s\n",
                "日期", "开盘", "收盘", "昨日收盘", "最高", "最低", "成交量", "成交额", "涨跌幅"));
        prompt.append("------------------------------------------------------------")
                .append("----------------------------------------------------\n");

        // 添加历史数据（按时间倒序，最近的在前）
        List<KLineData> reversedData = new ArrayList<>(historicalData);
        Collections.reverse(reversedData);

        for (KLineData data : reversedData) {
            prompt.append(String.format("%-12s %-8.3f %-8.3f %-8.3f %-8.3f %-8.3f %-12d %-12.0f %-6.2f%%\n",
                    sdf.format(data.getDate()),
                    data.getOpen(),
                    data.getClose(),
                    data.getLastClose(),
                    data.getHigh(),
                    data.getLow(),
                    data.getVolume(),
                    data.getAmount(),
                    data.getChangeRate()));
        }

        prompt.append("\n分析要求：\n");
        prompt.append("1. 重点关注并计算移动平均线、布林线等技术指标在下一个交易日因时间窗口滑动而产生的动态变化（例如，关键价格K线进出5/10/20均线[剔除假期]计算窗口指标对阻力拐点方向的影响）\n");
        prompt.append("2. 重点关注价格趋势、成交量变化和波动率变化\n");
        prompt.append("3. 综合考虑移动平均线、布林线、支撑位和阻力位等等的各个因素\n");
        prompt.append("4. 给出明确的涨跌预测（上涨/下跌/上影线横盘/下影线横盘）\n");
        prompt.append("5. 简要说明分析理由\n");

        prompt.append("\n请按照以下格式回复：\n");
        prompt.append("预测结果：[上涨/下跌/上影线横盘/下影线横盘]\n");
        prompt.append("置信度：[0-100]%\n");
        prompt.append("分析理由：[简要说明分析依据]\n");

        return prompt.toString();
    }

    /**
     * 批量生成默认数量的提示词
     */
    public List<StockAnalysisPrompt> generateDefaultPrompts(String stockCode) {
        return generateMultiplePrompts(stockCode, RANDOM_DAYS_COUNT);
    }
}