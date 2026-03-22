package com.fraud_detection.Fraud_Management.repository;

import com.fraud_detection.Fraud_Management.entity.AccountLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountLimitRepository extends JpaRepository<AccountLimit, Long> {
    Optional<AccountLimit> findByAccountNumber(String accountNumber);
}