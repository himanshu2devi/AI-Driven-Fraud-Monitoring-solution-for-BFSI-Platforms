package com.fraud_detection.Fraud_Management.repository;

import com.fraud_detection.Fraud_Management.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {

    long countByStatus(String status);



    List<TransactionLog> findTop5ByStatusOrderByTimestampDesc(String status);


}