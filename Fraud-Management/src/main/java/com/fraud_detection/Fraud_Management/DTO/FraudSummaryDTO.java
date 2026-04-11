package com.fraud_detection.Fraud_Management.DTO;

import java.util.List;
import java.util.Map;

public class FraudSummaryDTO {

    private long totalTransactions;
    private long fraudCount;
    private long alertCount;
    private double fraudRate;

    private Map<String, Long> statusDistribution;
    private List<Map<String, Object>> recentFrauds;
    private List<Map<String, Object>> topPatterns;

    // getters & setters

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getFraudCount() {
        return fraudCount;
    }

    public void setFraudCount(long fraudCount) {
        this.fraudCount = fraudCount;
    }

    public long getAlertCount() {
        return alertCount;
    }

    public void setAlertCount(long alertCount) {
        this.alertCount = alertCount;
    }

    public double getFraudRate() {
        return fraudRate;
    }

    public void setFraudRate(double fraudRate) {
        this.fraudRate = fraudRate;
    }

    public Map<String, Long> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(Map<String, Long> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public List<Map<String, Object>> getRecentFrauds() {
        return recentFrauds;
    }

    public void setRecentFrauds(List<Map<String, Object>> recentFrauds) {
        this.recentFrauds = recentFrauds;
    }

    public List<Map<String, Object>> getTopPatterns() {
        return topPatterns;
    }

    public void setTopPatterns(List<Map<String, Object>> topPatterns) {
        this.topPatterns = topPatterns;
    }
}