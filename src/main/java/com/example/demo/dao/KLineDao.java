package com.example.demo.dao;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * K线数据DAO接口[5](@ref)
 */
public interface KLineDao {

    /**
     * 根据股票代码和日期范围获取K线数据
     */
    List<KLineData> getKLineData(String stockCode, Date startDate, Date endDate);

    /**
     * 根据股票代码获取所有K线数据
     */
    List<KLineData> getAllKLineData(String stockCode);

    /**
     * 根据日期获取特定日期的K线数据
     */
    KLineData getKLineDataByDate(String stockCode, Date date);

    /**
     * 清空缓存
     */
    void clearCache();

    /**
     * 获取缓存统计信息
     */
    Map<String, Object> getCacheStats();
}