package com.example.demo.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


/**
 * K线数据实体类
 */
@Getter
@Setter
public class KLineData {
    @JSONField(name = "f51", format = "yyyy-MM-dd")
    private Date date;          // 日期

    @JSONField(name = "f52")
    private Double open;        // 开盘价

    @JSONField(name = "f53")
    private Double close;       // 收盘价

    @JSONField(name = "f54")
    private Double high;        // 最高价

    @JSONField(name = "f55")
    private Double low;         // 最低价

    @JSONField(name = "f56")
    private Long volume;        // 成交量

    @JSONField(name = "f57")
    private Double amount;      // 成交额

    @JSONField(name = "f58")
    private Double amplitude;   // 振幅

    @JSONField(name = "f59")
    private Double changeRate;  // 涨跌幅

    @JSONField(name = "f60")
    private Double changeAmount;// 涨跌额

    // 昨收价 = 今收盘价 (f53) - 涨跌额 (f60)
    private Double lastClose;

    @JSONField(name = "f61")
    private Double turnoverRate;// 换手率

    // 新增：K线周期
    private Integer klt = 101;  // 默认日K线

    // 构造方法
    public KLineData() {}

    public KLineData(String klineStr) {
        this(klineStr, 101); // 默认日K
    }

    public KLineData(String klineStr, int klt) {
        String[] parts = klineStr.split(",");
        if (parts.length >= 11) {
            this.date = java.sql.Date.valueOf(parts[0]);
            this.open = Double.parseDouble(parts[1]);
            this.close = Double.parseDouble(parts[2]);
            this.high = Double.parseDouble(parts[3]);
            this.low = Double.parseDouble(parts[4]);
            this.volume = Long.parseLong(parts[5]);
            this.amount = Double.parseDouble(parts[6]);
            this.amplitude = Double.parseDouble(parts[7]);
            this.changeRate = Double.parseDouble(parts[8]);
            this.changeAmount = Double.parseDouble(parts[9]);
            this.turnoverRate = Double.parseDouble(parts[10]);
            this.lastClose = close - changeAmount;
            this.klt = klt;
        }
    }

    public String getPeriodName() {
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

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}