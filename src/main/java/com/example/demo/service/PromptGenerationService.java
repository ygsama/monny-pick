package com.example.demo.service;

import com.example.demo.dao.KLineDao;
import com.example.demo.dao.KLineData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 多周期提示词生成服务
 */
@Service
public class PromptGenerationService {

    @Autowired
    private KLineDao kLineDao;

    private static final int RANDOM_DAYS_COUNT = 10;
    private static final Map<Integer, Integer> HISTORICAL_COUNT_MAP = new HashMap<>();

    static {
        HISTORICAL_COUNT_MAP.put(101, 30);  // 日K: 30个交易日
        HISTORICAL_COUNT_MAP.put(102, 30);  // 周K: 12周（约3个月）
        HISTORICAL_COUNT_MAP.put(103, 30);   // 月K: 6个月
        HISTORICAL_COUNT_MAP.put(104, 30);   // 季K: 4个季度
        HISTORICAL_COUNT_MAP.put(105, 30);   // 半年K: 3个
        HISTORICAL_COUNT_MAP.put(106, 30);   // 年K: 3年
    }

    private final Random random = new Random();

    // 原有的日K方法保持不变
    public List<StockAnalysisPrompt> generateDefaultPrompts(String stockCode) {
        return generateMultiplePrompts(stockCode, 101, RANDOM_DAYS_COUNT);
    }

    public List<StockAnalysisPrompt> generateMultiplePrompts(String stockCode, Date targetDate, int count) {
        return generateMultiplePrompts(stockCode, 101, targetDate, count);
    }

    /**
     * 生成多周期的分析提示词
     */
    public List<StockAnalysisPrompt> generateMultiplePrompts(String stockCode, int klt, int count) {
        List<StockAnalysisPrompt> prompts = new ArrayList<>();

        // 获取所有可用的日期
        List<Date> availableDates = getAvailableDatesWithSufficientHistory(stockCode, klt);
        if (availableDates.size() < count) {
            count = availableDates.size();
        }

        // 随机选择日期
        Collections.shuffle(availableDates);
        List<Date> selectedDates = availableDates.subList(0, Math.min(count, availableDates.size()));

        for (Date targetDate : selectedDates) {
            StockAnalysisPrompt prompt = generateSinglePrompt(stockCode, targetDate, klt);
            if (prompt != null) {
                prompts.add(prompt);
            }
        }

        return prompts;
    }

    /**
     * 生成多周期的分析提示词（指定目标日期）
     */
    public List<StockAnalysisPrompt> generateMultiplePrompts(String stockCode, int klt, Date targetDate, int count) {
        List<StockAnalysisPrompt> prompts = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            StockAnalysisPrompt prompt = generateSinglePrompt(stockCode, targetDate, klt);
            if (prompt != null) {
                prompts.add(prompt);
            }
        }

        return prompts;
    }

    /**
     * 生成单个日期的多周期分析提示词
     */
    public StockAnalysisPrompt generateSinglePrompt(String stockCode, Date targetDate, int klt) {
        try {
            int historicalCount = HISTORICAL_COUNT_MAP.getOrDefault(klt, 35);

            // 获取目标日期前N个周期的历史数据
            List<KLineData> historicalData = getHistoricalData(stockCode, targetDate, klt, historicalCount);
            if (historicalData == null || historicalData.size() < historicalCount) {
                return null;
            }

            // 获取下一个周期的实际数据（用于验证）
            KLineData nextPeriodData = getNextPeriodData(stockCode, targetDate, klt);
            boolean actualRise = nextPeriodData != null &&
                    nextPeriodData.getClose() > nextPeriodData.getLastClose();

            // 构建提示词
            String prompt = buildAnalysisPrompt(stockCode, targetDate, historicalData, klt);

            return new StockAnalysisPrompt(
                    stockCode, targetDate, historicalData, prompt, actualRise, klt
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取目标日期前N个周期的历史数据
     */
    private List<KLineData> getHistoricalData(String stockCode, Date targetDate, int klt, int periods) {
        return kLineDao.getRecentKLineData(stockCode, klt, periods * 2).stream()
                .filter(data -> !data.getDate().after(targetDate))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(periods)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取下一个周期的数据
     */
    private KLineData getNextPeriodData(String stockCode, Date targetDate, int klt) {
        List<KLineData> allData = kLineDao.getAllKLineData(stockCode, klt);
        if (allData == null || allData.isEmpty()) {
            return null;
        }

        // 按日期排序
        allData.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        for (int i = 0; i < allData.size() - 1; i++) {
            if (allData.get(i).getDate().equals(targetDate)) {
                return allData.get(i + 1);
            }
        }

        return null;
    }

    /**
     * 获取所有有足够历史数据的可用日期
     */
    private List<Date> getAvailableDatesWithSufficientHistory(String stockCode, int klt) {
        List<Date> availableDates = new ArrayList<>();

        // 获取所有数据
        List<KLineData> allData = kLineDao.getAllKLineData(stockCode, klt);
        int requiredCount = HISTORICAL_COUNT_MAP.getOrDefault(klt, 30);

        if (allData == null || allData.size() <= requiredCount) {
            return availableDates;
        }

        // 按日期排序（从早到晚）
        allData.sort((a, b) -> a.getDate().compareTo(b.getDate()));

        // 确保有足够的历史数据和后续验证数据
        for (int i = requiredCount; i < allData.size() - 1; i++) {
            availableDates.add(allData.get(i).getDate());
        }

        return availableDates;
    }

    /**
     * 构建多周期分析提示词
     */
    private String buildAnalysisPrompt(String stockCode, Date targetDate,
                                       List<KLineData> historicalData, int klt) {
        StringBuilder prompt = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String periodName = getPeriodName(klt);

        prompt.append("你是一个数学专业出身的股票实盘大赛冠军，请分析以下股票历史数据，预测下一个").append(periodName).append("周期的涨跌情况：\n\n");
        prompt.append("股票代码：").append(stockCode).append("\n");
        prompt.append("分析日期：").append(sdf.format(targetDate)).append("\n");
        prompt.append("K线周期：").append(periodName).append("\n");
        prompt.append("历史数据（最近").append(historicalData.size()).append("个").append(periodName).append("）：\n");

        // 添加表头
        prompt.append(String.format("%-12s %-8s %-8s %-8s %-8s %-8s %-12s %-12s %-6s %-8s\n",
                "日期", "开盘", "收盘", "昨收", "最高", "最低", "成交量", "成交额", "涨跌幅", "周期"));
        prompt.append("-------------------------------------------------------------\n");

        // 添加历史数据（按时间倒序，最近的在前）
        List<KLineData> reversedData = new ArrayList<>(historicalData);
        Collections.reverse(reversedData);

        for (KLineData data : reversedData) {
            prompt.append(String.format("%-12s %-8.3f %-8.3f %-8.3f %-8.3f %-8.3f %-12d %-12.0f %-6.2f%% %-8s\n",
                    sdf.format(data.getDate()),
                    data.getOpen(),
                    data.getClose(),
                    data.getLastClose(),
                    data.getHigh(),
                    data.getLow(),
                    data.getVolume(),
                    data.getAmount(),
                    data.getChangeRate(),
                    data.getPeriodName()));
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

    private String getPeriodName(int klt) {
        switch (klt) {
            case 101: return "日K线";
            case 102: return "周K线";
            case 103: return "月K线";
            case 104: return "季K线";
            case 105: return "半年K线";
            case 106: return "年K线";
            default: return "未知周期";
        }
    }
}