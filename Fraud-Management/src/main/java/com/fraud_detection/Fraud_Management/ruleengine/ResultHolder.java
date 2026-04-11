package com.fraud_detection.Fraud_Management.ruleengine;


public class ResultHolder {
    private TransactionStatus status;
    private String reason;

    private double ruleScore;
    private double aiScore;
    private double finalScore;
    private String riskLevel;

    public ResultHolder(TransactionStatus status, String reason, double ruleScore, double aiScore, double finalScore, String riskLevel) {
        this.status = status;
        this.reason = reason;
        this.ruleScore = ruleScore;
        this.aiScore = aiScore;
        this.finalScore = finalScore;
        this.riskLevel = riskLevel;
    }

    public ResultHolder() {

    }

    public double getRuleScore() {
        return ruleScore;
    }

    public void setRuleScore(double ruleScore) {
        this.ruleScore = ruleScore;
    }

    public double getAiScore() {
        return aiScore;
    }

    public void setAiScore(double aiScore) {
        this.aiScore = aiScore;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(double finalScore) {
        this.finalScore = finalScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
