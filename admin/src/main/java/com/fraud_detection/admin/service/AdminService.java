package com.fraud_detection.admin.service;


import com.fraud_detection.admin.entity.TransactionLogEntity;
import com.fraud_detection.admin.repository.TransactionLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    private final TransactionLogRepository transactionLogRepository;

    public AdminService(TransactionLogRepository transactionLogRepository) {
        this.transactionLogRepository = transactionLogRepository;
    }

    // Get transactions by status
    public List<TransactionLogEntity> getByStatus(String status) {
        return transactionLogRepository.findByStatus(status);
    }

    // Get all transactions
    public List<TransactionLogEntity> getAllTransactions() {
        return transactionLogRepository.findAll();
    }

    // Update only the 'caseOpened' field
    public TransactionLogEntity updateCaseOpen(String transactionId, String caseOpenStatus) {
        TransactionLogEntity transaction = transactionLogRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));
        transaction.setCaseOpened(caseOpenStatus);
        return transactionLogRepository.save(transaction);
    }

    // Update status (from ALERT to FRAUD) and optionally update caseOpened
    public TransactionLogEntity updateStatusAndCaseOpen(String transactionId, String caseOpenStatus) {
        TransactionLogEntity transaction = transactionLogRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found with ID: " + transactionId));

        if ("ALERT".equalsIgnoreCase(transaction.getStatus())) {
            transaction.setStatus("FRAUD");
        }

        if (caseOpenStatus != null) {
            transaction.setCaseOpened(caseOpenStatus);
        }

        return transactionLogRepository.save(transaction);
    }
}
