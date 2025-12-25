package com.example.demo.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * K线数据DAO接口
 */
public interface KLineDao {

    // 原有方法 - 日K线
    List<KLineData> getKLineData(String stockCode, Date startDate, Date endDate);
    List<KLineData> getAllKLineData(String stockCode);
    KLineData getKLineDataByDate(String stockCode, Date date);

    // 新增方法 - 支持多周期
    List<KLineData> getKLineData(String stockCode, Date startDate, Date endDate, int klt);
    List<KLineData> getAllKLineData(String stockCode, int klt);
    KLineData getKLineDataByDate(String stockCode, Date date, int klt);

    // 获取最近N条K线数据
    List<KLineData> getRecentKLineData(String stockCode, int klt, int count);

    // 缓存管理
    void clearCache();
    void clearCache(String stockCode);
    void clearCache(String stockCode, int klt);

    // 缓存统计
    Map<String, Object> getCacheStats();
}