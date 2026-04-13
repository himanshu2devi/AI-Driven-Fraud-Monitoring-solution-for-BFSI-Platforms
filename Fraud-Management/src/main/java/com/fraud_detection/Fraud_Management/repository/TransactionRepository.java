package com.fraud_detection.Fraud_Management.repository;

import com.fraud_detection.Fraud_Management.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    long countByStatus(String status);

}
