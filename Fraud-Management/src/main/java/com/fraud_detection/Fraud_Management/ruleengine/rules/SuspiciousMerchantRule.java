package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.entity.SuspiciousMerchant;
import com.fraud_detection.Fraud_Management.repository.SuspiciousMerchantRepository;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SuspiciousMerchantRule implements TransactionRule {

    private final SuspiciousMerchantRepository suspiciousMerchantRepository;

    @Autowired
    public SuspiciousMerchantRule(SuspiciousMerchantRepository suspiciousMerchantRepository) {
        this.suspiciousMerchantRepository = suspiciousMerchantRepository;
    }

    @Override
    public TransactionResult apply(TransactionDTO txn) {
        // Check if the recipient account is in the suspicious merchants table
        Optional<SuspiciousMerchant> suspiciousMerchant = suspiciousMerchantRepository.findByAccountNumber(txn.getAccNoTo());

        if (suspiciousMerchant.isPresent()) {
            return new TransactionResult(TransactionStatus.ALERT, "Transaction to suspicious merchant.");
        }

        return new TransactionResult(TransactionStatus.VALID, "Merchant clean.");
    }
}