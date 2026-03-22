package com.fraud_detection.Fraud_Management.repository;

import com.fraud_detection.Fraud_Management.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    // You can add custom methods here if needed (e.g., findByTransactionId)
}