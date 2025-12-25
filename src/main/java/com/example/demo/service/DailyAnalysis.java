package com.example.demo.service;

/**
 * 单日分析结果
 */
class DailyAnalysis {
    private int daysBefore;           // T-n
    private boolean isRise;          // 当日是否上涨
    private boolean isVolumeShrink;  // 是否缩量
    private double weight;           // 权重
    private boolean signalRise;     // 信号方向（true涨，false跌）
    private double signalStrength;  // 信号强度

    public DailyAnalysis(int daysBefore, boolean isRise, boolean isVolumeShrink,
                         double weight, boolean signalRise, double signalStrength) {
        this.daysBefore = daysBefore;
        this.isRise = isRise;
        this.isVolumeShrink = isVolumeShrink;
        this.weight = weight;
        this.signalRise = signalRise;
        this.signalStrength = signalStrength;
    }

    // Getter方法
    public int getDaysBefore() { return daysBefore; }
    public boolean isRise() { return isRise; }
    public boolean isVolumeShrink() { return isVolumeShrink; }
    public double getWeight() { return weight; }
    public boolean isSignalRise() { return signalRise; }
    public double getSignalStrength() { return signalStrength; }
}