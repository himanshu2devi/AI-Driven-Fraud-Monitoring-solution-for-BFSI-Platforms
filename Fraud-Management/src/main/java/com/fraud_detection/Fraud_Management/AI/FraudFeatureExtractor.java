package com.fraud_detection.Fraud_Management.AI;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FraudFeatureExtractor {

    //  in-memory transaction tracking
    // key = account number
    // value = last transaction timestamp
    private final Map<String, LocalDateTime> recentTransactions = new HashMap<>();

    public List<Double> extract(TransactionDTO tx) {

        double amount = tx.getAmount();

        // =========================================
        // 1️ INTERNATIONAL TRANSACTION
        // =========================================
        double isInternational =
                "INR".equalsIgnoreCase(tx.getCurrency()) ? 0.0 : 1.0;

        // =========================================
        // 2️ NIGHT TRANSACTION
        // =========================================
        int hour = tx.getTimestamp().getHour();

        double isNight =
                (hour >= 0 && hour < 5) ? 1.0 : 0.0;

        // =========================================
        // 3️ WEEKEND TRANSACTION
        // =========================================
        DayOfWeek day = tx.getTimestamp().getDayOfWeek();

        double isWeekend =
                (day == DayOfWeek.SATURDAY
                        || day == DayOfWeek.SUNDAY)
                        ? 1.0 : 0.0;

        // =========================================
        // 4️ TRANSACTION VELOCITY
        // =========================================
        double velocity5min = calculateVelocity(tx);

        // =========================================
        // 5️ NEW MERCHANT / NEW BENEFICIARY
        // =========================================
        double isNewMerchant = calculateNewBeneficiary(tx);

        // =========================================
        // 6️ ACCOUNT AGE (REALISTIC HEURISTIC)
        // =========================================
        double accountAgeDays = estimateAccountAge(tx);

        // =========================================
        // 7️ FAILED LOGIN ATTEMPTS
        // =========================================
        double failedLoginAttempts = estimateLoginRisk(tx);

        return List.of(
                amount,
                isInternational,
                isNight,
                isWeekend,
                velocity5min,
                isNewMerchant,
                accountAgeDays,
                failedLoginAttempts
        );
    }

    // =========================================
    //  Velocity Detection
    // =========================================
    private double calculateVelocity(TransactionDTO tx) {

        String accNo = tx.getAccNoFrom();

        LocalDateTime now = tx.getTimestamp();

        if (recentTransactions.containsKey(accNo)) {

            LocalDateTime previous = recentTransactions.get(accNo);

            long seconds =
                    java.time.Duration.between(previous, now).getSeconds();

            recentTransactions.put(accNo, now);

            // multiple tx within 1 min
            if (seconds < 60) {
                return 8.0;
            }
        }

        recentTransactions.put(accNo, now);

        return 2.0;
    }

    // =========================================
    //  New Beneficiary Detection
    // =========================================
    private double calculateNewBeneficiary(TransactionDTO tx) {

        if (tx.getAccNoTo() == null) {
            return 0.0;
        }

        // simple realistic heuristic
        if (tx.getAccNoTo().startsWith("NEW")) {
            return 1.0;
        }

        return 0.0;
    }

    // =========================================
    //  Account Age Estimation
    // =========================================
    private double estimateAccountAge(TransactionDTO tx) {

        // realistic temporary heuristic

        if (tx.getAmount() > 50000) {
            return 90.0;
        }

        return 800.0;
    }

    // =========================================
    //  Login Risk Estimation
    // =========================================
    private double estimateLoginRisk(TransactionDTO tx) {

        // temporary behavioral heuristic

        if ("credit_card".equalsIgnoreCase(tx.getSourceType())) {
            return 2.0;
        }

        return 0.0;
    }
}