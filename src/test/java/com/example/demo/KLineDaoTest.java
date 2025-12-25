package com.example.demo;

import com.example.demo.dao.JsonUtil;
import com.example.demo.dao.KLineDao;
import com.example.demo.dao.KLineDaoImpl;
import com.example.demo.dao.KLineData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 测试类
 */
public class KLineDaoTest {
    public static void main(String[] args) throws Exception {
        KLineDao klineDao = new KLineDaoImpl();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date startDate = sdf.parse("2024-01-01");
        Date endDate = sdf.parse("2024-12-31");

        // 测试获取沪深300ETF数据
        List<KLineData> klineData = klineDao.getKLineData("510300", startDate, endDate);

        if (klineData != null) {
            System.out.println("获取到 " + klineData.size() + " 条K线数据");

            // 打印前5条数据
            for (int i = 0; i < Math.min(5, klineData.size()); i++) {
                KLineData data = klineData.get(i);
                System.out.println(JsonUtil.toJsonString(data));
            }

            // 测试缓存功能
            System.out.println("\n=== 测试缓存 ===");
            long startTime = System.currentTimeMillis();
            List<KLineData> cachedData = klineDao.getKLineData("510300", startDate, endDate);
            long endTime = System.currentTimeMillis();
            System.out.println("缓存读取耗时: " + (endTime - startTime) + "ms");

            // 显示缓存统计
            System.out.println("缓存统计: " + klineDao.getCacheStats());
        }
    }
}