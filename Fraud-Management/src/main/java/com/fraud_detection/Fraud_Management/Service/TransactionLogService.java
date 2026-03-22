package com.fraud_detection.Fraud_Management.service;

import com.fraud_detection.Fraud_Management.entity.TransactionLog;
import com.fraud_detection.Fraud_Management.repository.TransactionLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionLogService {

    private final TransactionLogRepository transactionLogRepository;

    public TransactionLogService(TransactionLogRepository transactionLogRepository) {
        this.transactionLogRepository = transactionLogRepository;
    }

    public TransactionLog saveTransactionLog(String transactionId,
                                                         String accountFrom,
                                                         String accountTo,
                                                         Double amount,
                                                         String transactionType,
                                                         String status,
                                                         String reason) {
        TransactionLog log = new TransactionLog();
        log.setTransactionId(transactionId);
        log.setAccountFrom(accountFrom);
        log.setAccountTo(accountTo);
        log.setAmount(amount);
        log.setTransactionType(transactionType);
        log.setStatus(status);
        log.setReason(reason);
        log.setTimestamp(LocalDateTime.now());

        return transactionLogRepository.save(log);
    }


    public void createTransactionLog(String transactionId, String accNoFrom, String accNoTo,
                                     Double amount, String transactionType,
                                     String status, String reason) {

        // Create the Transaction Log entry
        TransactionLog transactionLog = new TransactionLog();

        transactionLog.setTransactionId(transactionId);
        transactionLog.setAccountFrom(accNoFrom);
        transactionLog.setAccountTo(accNoTo);
        transactionLog.setAmount(amount);
        transactionLog.setTransactionType(transactionType);
        transactionLog.setStatus(status); // VALID, ALERT, FRAUD
        transactionLog.setReason(reason);
        transactionLog.setTimestamp(LocalDateTime.now()); // Set current timestamp

        // Save it to the database
        transactionLogRepository.save(transactionLog);
    }
}