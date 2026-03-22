package com.fraud_detection.admin.repository;


import com.fraud_detection.admin.entity.TransactionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLogEntity, String> {

    // Find transactions by status
    List<TransactionLogEntity> findByStatus(String status);

    // Find a single transaction by transactionId
    Optional<TransactionLogEntity> findByTransactionId(String transactionId);
}
