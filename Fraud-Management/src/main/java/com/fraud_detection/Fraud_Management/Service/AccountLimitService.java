package com.fraud_detection.Fraud_Management.service;

import com.fraud_detection.Fraud_Management.entity.AccountLimit;
import com.fraud_detection.Fraud_Management.repository.AccountLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountLimitService {

    private final AccountLimitRepository accountLimitRepository;

    public AccountLimitService(AccountLimitRepository accountLimitRepository) {
        this.accountLimitRepository = accountLimitRepository;
    }

    public Optional<AccountLimit> getLimitsByAccountNumber(String accountNumber) {
        return accountLimitRepository.findByAccountNumber(accountNumber);
    }

    public AccountLimit setOrUpdateLimits(String accountNumber, Double dailyLimit, Double transactionLimit) {
        Optional<AccountLimit> existingLimit = accountLimitRepository.findByAccountNumber(accountNumber);

        AccountLimit limit = existingLimit.orElseGet(AccountLimit::new);
        limit.setAccountNumber(accountNumber);
        limit.setDailyLimit(dailyLimit);
        limit.setTransactionLimit(transactionLimit);

        return accountLimitRepository.save(limit);
    }
}