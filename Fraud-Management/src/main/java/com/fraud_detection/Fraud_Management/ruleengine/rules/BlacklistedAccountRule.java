package com.fraud_detection.Fraud_Management.ruleengine.rules;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.repository.BlacklistedAccountRepository;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRule;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class BlacklistedAccountRule implements TransactionRule {

    private final BlacklistedAccountRepository blacklistedAccountRepository;

    public BlacklistedAccountRule(BlacklistedAccountRepository blacklistedAccountRepository) {
        this.blacklistedAccountRepository = blacklistedAccountRepository;
    }

    @Override
    public TransactionResult apply(TransactionDTO txn) {
        boolean fromAccountBlacklisted = blacklistedAccountRepository.existsByAccountNumber(txn.getAccNoFrom());
        boolean toAccountBlacklisted = blacklistedAccountRepository.existsByAccountNumber(txn.getAccNoTo());

        if (fromAccountBlacklisted || toAccountBlacklisted) {
            return new TransactionResult(TransactionStatus.FRAUD, "Blacklisted account involved in transaction.");
        }
        return new TransactionResult(TransactionStatus.VALID, "Accounts are clean.");
    }
}