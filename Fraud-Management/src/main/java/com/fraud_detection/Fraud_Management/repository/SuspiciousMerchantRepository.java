package com.fraud_detection.Fraud_Management.repository;

import com.fraud_detection.Fraud_Management.entity.SuspiciousMerchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuspiciousMerchantRepository extends JpaRepository<SuspiciousMerchant, Long> {
    Optional<SuspiciousMerchant> findByAccountNumber(String accountNumber);
}