package com.fraud_detection.Fraud_Management.Service;

import com.fraud_detection.Fraud_Management.entity.BlacklistedAccount;
import com.fraud_detection.Fraud_Management.repository.BlacklistedAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BlacklistService {

    private final BlacklistedAccountRepository blacklistedAccountRepository;

    public BlacklistService(BlacklistedAccountRepository blacklistedAccountRepository) {
        this.blacklistedAccountRepository = blacklistedAccountRepository;
    }

    public void blacklistAccount(String accountNumber, String reason) {
        BlacklistedAccount account = new BlacklistedAccount();
        account.setAccountNumber(accountNumber);
        account.setReason(reason);
        account.setBlockedAt(LocalDateTime.now());

        blacklistedAccountRepository.save(account);
    }

    public boolean isBlacklisted(String accountNumber) {
        return blacklistedAccountRepository.findByAccountNumber(accountNumber).isPresent();
    }
}