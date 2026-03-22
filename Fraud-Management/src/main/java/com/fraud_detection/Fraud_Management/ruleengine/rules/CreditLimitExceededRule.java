package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.entity.AccountLimit;
import com.fraud_detection.Fraud_Management.repository.AccountLimitRepository;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CreditLimitExceededRule implements TransactionRule {

    private final AccountLimitRepository accountLimitRepository;

    @Autowired
    public CreditLimitExceededRule(AccountLimitRepository accountLimitRepository) {
        this.accountLimitRepository = accountLimitRepository;
    }

    @Override
    public TransactionResult apply(TransactionDTO txn) {
        if (!"credit".equalsIgnoreCase(txn.getTransactionType())) {
            return new TransactionResult(TransactionStatus.VALID, "Not a credit transaction.");
        }

        // Fetch the AccountLimit object from the database based on the account number
        Optional<AccountLimit> accountLimitOptional = accountLimitRepository.findByAccountNumber(txn.getAccNoFrom());

        if (!accountLimitOptional.isPresent()) {
            return new TransactionResult(TransactionStatus.FRAUD, "Account not found.");
        }

        // Access the AccountLimit from the Optional
        AccountLimit accountLimit = accountLimitOptional.get();

        if (txn.getAmount() > accountLimit.getTransactionLimit()) {
            return new TransactionResult(TransactionStatus.FRAUD, "Transaction exceeds credit card limit.");
        }

        return new TransactionResult(TransactionStatus.VALID, "Credit limit within range.");
    }
}