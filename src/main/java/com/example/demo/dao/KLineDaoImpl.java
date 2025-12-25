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
 * K线数据DAO实现类，包含HashMap缓存
 * 专门用于日线数据，移除了缓存淘汰策略
 */
@Service
public class KLineDaoImpl implements KLineDao {

    // 缓存数据结构：Map<股票代码, Map<日期, KLineData>>
    private final Map<String, Map<String, KLineData>> cacheMap = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public List<KLineData> getKLineData(String stockCode, Date startDate, Date endDate) {

        // 检查缓存是否存在该股票的数据
        Map<String, KLineData> stockCache = cacheMap.get(stockCode);
        if (stockCache != null) {
            // 从缓存中筛选指定日期范围的数据
            List<KLineData> result = filterDataByDateRange(stockCache, startDate, endDate);
            if (!result.isEmpty() && isCacheComplete(stockCache, startDate, endDate)) {
                return result;
            }
        }

        // 缓存未命中，从API获取数据
        List<KLineData> klineData = fetchFromAPI("1." + stockCode, startDate, endDate);
        if (klineData == null) {
            klineData = fetchFromAPI("0." + stockCode, startDate, endDate);
        }
        if (klineData != null) {
            // 更新缓存
            updateCache(stockCode, klineData);
        }

        return klineData;
    }

    /**
     * 缓存数据不完整
     */
    private boolean isCacheComplete(Map<String, KLineData> stockCache, Date startDate, Date endDate) {
        if (stockCache == null || stockCache.isEmpty()) {
            return false;
        }

        // 找出缓存中的最早和最晚日期
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

        // 检查缓存是否完全覆盖请求范围
        // 只有当缓存的起始日期 <= 请求起始日期 且 缓存的结束日期 >= 请求结束日期时
        // 才认为缓存完整
        return !startDate.before(earliestCacheDate) && !endDate.after(latestCacheDate);
    }

    @Override
    public List<KLineData> getAllKLineData(String stockCode) {
        // 获取最近两年的数据
        Calendar calendar = Calendar.getInstance();
        Date endDate = calendar.getTime();
        calendar.add(Calendar.YEAR, -1);
        Date startDate = calendar.getTime();

        return getKLineData(stockCode, startDate, endDate);
    }

    @Override
    public KLineData getKLineDataByDate(String stockCode, Date date) {
        Map<String, KLineData> stockCache = cacheMap.get(stockCode);
        if (stockCache != null) {
            String dateKey = dateFormat.format(date);
            return stockCache.get(dateKey);
        }

        // 如果缓存中没有，尝试从API获取该日期附近的数据
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -10); // 获取前后10天的数据
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 10 + 10);
        Date endDate = calendar.getTime();

        List<KLineData> data = getKLineData(stockCode, startDate, endDate);
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
    public void clearCache() {
        cacheMap.clear();
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("股票数量", cacheMap.size());

        int totalDataPoints = 0;
        for (Map<String, KLineData> stockData : cacheMap.values()) {
            totalDataPoints += stockData.size();
        }
        stats.put("日K数量", totalDataPoints);
        stats.put("股票列表", new ArrayList<>(cacheMap.keySet()));

        return stats;
    }

    /**
     * 从东方财富API获取K线数据
     */
    private List<KLineData> fetchFromAPI(String stockCode, Date startDate, Date endDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String beg = sdf.format(startDate);
            String end = sdf.format(endDate);

            String urlStr = "https://push2his.eastmoney.com/api/qt/stock/kline/get?" +
                    "secid=" + stockCode +
                    "&fields1=f1,f2,f3,f4,f5,f6" +
                    "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
                    "&klt=101" + // 101=日K线。102=周K线。103=月K线。104=季K 105=半年K 106=年K
                    "&fqt=1" +   //  复权类型 1=前复权
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
                    return parseKLineData(klines);
                }
            } else {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 解析K线数据字符串列表
     */
    private List<KLineData> parseKLineData(List<String> klines) {
        List<KLineData> result = new ArrayList<>();
        for (String klineStr : klines) {
            result.add(new KLineData(klineStr));
        }
        // 按日期排序（最新的在前）
        result.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return result;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String stockCode, Date startDate, Date endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return stockCode + "_" + sdf.format(startDate) + "_" + sdf.format(endDate);
    }

    /**
     * 更新缓存：将数据按日期存储
     */
    private void updateCache(String stockCode, List<KLineData> newData) {
        Map<String, KLineData> stockCache = cacheMap.computeIfAbsent(stockCode, k -> new ConcurrentHashMap<>());

        for (KLineData data : newData) {
            String dateKey = dateFormat.format(data.getDate());
            stockCache.put(dateKey, data);
        }
    }

    /**
     * 从缓存中筛选指定日期范围的数据
     */
    private List<KLineData> filterDataByDateRange(Map<String, KLineData> stockCache, Date startDate, Date endDate) {
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
}