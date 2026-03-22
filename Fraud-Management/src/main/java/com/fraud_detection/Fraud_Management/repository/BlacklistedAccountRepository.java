package com.fraud_detection.Fraud_Management.repository;

import com.fraud_detection.Fraud_Management.entity.BlacklistedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistedAccountRepository extends JpaRepository<BlacklistedAccount, Long> {
    Optional<BlacklistedAccount> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);

}