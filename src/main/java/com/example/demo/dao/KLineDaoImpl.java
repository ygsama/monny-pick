package com.example.demo.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * K线数据DAO实现类，支持多周期缓存
 */
@Service
public class KLineDaoImpl implements KLineDao {

    /**
     * 缓存数据结构：Map<股票代码, Map<周期, Map<日期, KLineData>>>
      */
    private final Map<String, Map<Integer, Map<String, KLineData>>> cacheMap = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // 默认获取日K线
    @Override
    public List<KLineData> getKLineData(String stockCode, Date startDate, Date endDate) {
        return getKLineData(stockCode, startDate, endDate, 101);
    }

    @Override
    public List<KLineData> getAllKLineData(String stockCode) {
        return getAllKLineData(stockCode, 101);
    }

    @Override
    public KLineData getKLineDataByDate(String stockCode, Date date) {
        return getKLineDataByDate(stockCode, date, 101);
    }

    // 实现新增的多周期方法
    @Override
    public List<KLineData> getKLineData(String stockCode, Date startDate, Date endDate, int klt) {
        String stockKey = getStockKey(stockCode, klt);

        // 检查缓存是否存在该股票该周期的数据
        Map<String, KLineData> periodCache = getPeriodCache(stockCode, klt);
        if (periodCache != null) {
            // 从缓存中筛选指定日期范围的数据
            List<KLineData> result = filterDataByDateRange(periodCache, startDate, endDate);
            if (!result.isEmpty() && isCacheComplete(periodCache, startDate, endDate)) {
                return result;
            }
        }

        // 缓存未命中，从API获取数据
        List<KLineData> klineData = fetchFromAPI(stockCode, startDate, endDate, klt);
        if (klineData != null) {
            // 更新缓存
            updateCache(stockCode, klt, klineData);
        }

        return klineData;
    }

    @Override
    public List<KLineData> getAllKLineData(String stockCode, int klt) {
        // 根据周期获取不同时间范围的数据
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();

        switch (klt) {
            case 101: // 日K - 获取最近一年
                calendar.add(Calendar.YEAR, -1);
                break;
            case 102: // 周K - 获取最近两年
                calendar.add(Calendar.YEAR, -2);
                break;
            case 103: // 月K - 获取最近五年
                calendar.add(Calendar.YEAR, -5);
                break;
            case 104: // 季K - 获取最近八年
                calendar.add(Calendar.YEAR, -8);
                break;
            case 105: // 半年K - 获取最近十年
                calendar.add(Calendar.YEAR, -10);
                break;
            case 106: // 年K - 获取最近二十年
                calendar.add(Calendar.YEAR, -20);
                break;
            default:
                calendar.add(Calendar.YEAR, -1);
        }

        Date startDate = calendar.getTime();
        return getKLineData(stockCode, startDate, endDate, klt);
    }

    @Override
    public KLineData getKLineDataByDate(String stockCode, Date date, int klt) {
        Map<String, KLineData> periodCache = getPeriodCache(stockCode, klt);
        if (periodCache != null) {
            String dateKey = dateFormat.format(date);
            return periodCache.get(dateKey);
        }

        // 如果缓存中没有，尝试从API获取该日期附近的数据
        int daysOffset = getDaysOffsetByKlt(klt);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -daysOffset);
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, daysOffset * 2);
        Date endDate = calendar.getTime();

        List<KLineData> data = getKLineData(stockCode, startDate, endDate, klt);
        if (data != null) {
            for (KLineData kline : data) {
                if (dateFormat.format(kline.getDate()).equals(dateFormat.format(date))) {
                    return kline;
                }
            }
        }
        return null;
    }

    @Override
    public List<KLineData> getRecentKLineData(String stockCode, int klt, int count) {
        List<KLineData> allData = getAllKLineData(stockCode, klt);
        if (allData == null || allData.size() < count) {
            return allData;
        }
        return allData.subList(0, Math.min(count, allData.size()));
    }

    @Override
    public void clearCache() {
        cacheMap.clear();
    }

    @Override
    public void clearCache(String stockCode) {
        cacheMap.remove(stockCode);
    }

    @Override
    public void clearCache(String stockCode, int klt) {
        Map<Integer, Map<String, KLineData>> stockCache = cacheMap.get(stockCode);
        if (stockCache != null) {
            stockCache.remove(klt);
        }
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("股票数量", cacheMap.size());

        Map<String, Integer> periodStats = new HashMap<>();
        int totalDataPoints = 0;

        for (Map.Entry<String, Map<Integer, Map<String, KLineData>>> stockEntry : cacheMap.entrySet()) {
            for (Map.Entry<Integer, Map<String, KLineData>> periodEntry : stockEntry.getValue().entrySet()) {
                String periodName = getPeriodName(periodEntry.getKey());
                int dataCount = periodEntry.getValue().size();
                periodStats.put(periodName, periodStats.getOrDefault(periodName, 0) + dataCount);
                totalDataPoints += dataCount;
            }
        }

        stats.put("K线数量", totalDataPoints);
        stats.put("周期分布", periodStats);

        return stats;
    }

    /**
     * 从东方财富API获取K线数据
     */
    private List<KLineData> fetchFromAPI(String stockCode, Date startDate, Date endDate, int klt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String beg = sdf.format(startDate);
            String end = sdf.format(endDate);

            // 尝试两种市场前缀
            String[] marketPrefixes = {"1.", "0."}; // 1=沪市, 0=深市
            List<KLineData> result = null;

            for (String prefix : marketPrefixes) {
                result = fetchWithMarketPrefix(prefix + stockCode, beg, end, klt);
                if (result != null && !result.isEmpty()) {
                    break;
                }
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<KLineData> fetchWithMarketPrefix(String secid, String beg, String end, int klt) {
        try {
            String urlStr = "https://push2his.eastmoney.com/api/qt/stock/kline/get?" +
                    "secid=" + secid +
                    "&fields1=f1,f2,f3,f4,f5,f6" +
                    "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
                    "&klt=" + klt + // 101=日K线。102=周K线。103=月K线。104=季K 105=半年K 106=年K
                    "&fqt=1" +   // 复权类型 1=前复权
                    "&beg=" + beg +
                    "&end=" + end;

            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = JSON.parseObject(response.toString());
            if (jsonResponse.getInteger("rc") == 0) {
                JSONObject data = jsonResponse.getJSONObject("data");
                if (data != null) {
                    List<String> klines = data.getJSONArray("klines").toJavaList(String.class);
                    return parseKLineData(klines, klt);
                }
            }
        } catch (Exception e) {
            // 记录错误但继续尝试其他市场前缀
        }
        return null;
    }

    /**
     * 解析K线数据字符串列表
     */
    private List<KLineData> parseKLineData(List<String> klines, int klt) {
        List<KLineData> result = new ArrayList<>();
        for (String klineStr : klines) {
            result.add(new KLineData(klineStr, klt));
        }
        // 按日期排序（最新的在前）
        result.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return result;
    }

    /**
     * 获取周期的缓存
     */
    private Map<String, KLineData> getPeriodCache(String stockCode, int klt) {
        Map<Integer, Map<String, KLineData>> stockCache = cacheMap.get(stockCode);
        if (stockCache == null) {
            return null;
        }
        return stockCache.get(klt);
    }

    /**
     * 更新缓存
     */
    private void updateCache(String stockCode, int klt, List<KLineData> newData) {
        Map<Integer, Map<String, KLineData>> stockCache =
                cacheMap.computeIfAbsent(stockCode, k -> new ConcurrentHashMap<>());

        Map<String, KLineData> periodCache =
                stockCache.computeIfAbsent(klt, k -> new ConcurrentHashMap<>());

        for (KLineData data : newData) {
            String dateKey = dateFormat.format(data.getDate());
            periodCache.put(dateKey, data);
        }
    }

    /**
     * 从缓存中筛选指定日期范围的数据
     */
    private List<KLineData> filterDataByDateRange(Map<String, KLineData> stockCache,
                                                  Date startDate, Date endDate) {
        List<KLineData> result = new ArrayList<>();

        for (KLineData data : stockCache.values()) {
            if (!data.getDate().before(startDate) && !data.getDate().after(endDate)) {
                result.add(data);
            }
        }

        // 按日期排序（最新的在前）
        result.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return result;
    }

    /**
     * 检查缓存是否完整覆盖请求范围
     */
    private boolean isCacheComplete(Map<String, KLineData> stockCache, Date startDate, Date endDate) {
        if (stockCache == null || stockCache.isEmpty()) {
            return false;
        }

        Date earliestCacheDate = null;
        Date latestCacheDate = null;

        for (KLineData data : stockCache.values()) {
            Date dataDate = data.getDate();
            if (earliestCacheDate == null || dataDate.before(earliestCacheDate)) {
                earliestCacheDate = dataDate;
            }
            if (latestCacheDate == null || dataDate.after(latestCacheDate)) {
                latestCacheDate = dataDate;
            }
        }

        if (earliestCacheDate == null || latestCacheDate == null) {
            return false;
        }

        return !startDate.before(earliestCacheDate) && !endDate.after(latestCacheDate);
    }

    /**
     * 根据K线周期获取日期偏移量
     */
    private int getDaysOffsetByKlt(int klt) {
        switch (klt) {
            case 101: return 10;  // 日K
            case 102: return 70;  // 周K
            case 103: return 365; // 月K
            case 104: return 1095; // 季K
            case 105: return 1825; // 半年K
            case 106: return 3650; // 年K
            default: return 10;
        }
    }

    /**
     * 获取周期名称
     */
    private String getPeriodName(int klt) {
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

    /**
     * 生成股票缓存键
     */
    private String getStockKey(String stockCode, int klt) {
        return stockCode + "_" + klt;
    }
}