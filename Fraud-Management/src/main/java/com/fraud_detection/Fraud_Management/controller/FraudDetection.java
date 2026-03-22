package com.fraud_detection.Fraud_Management.controller;

import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.Kafka.TransactionProducer;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.Service.FraudAlertService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
public class FraudDetection {

    private final TransactionProducer transactionProducer;
    private final com.fraud_detection.Fraud_Management.service.TransactionLogService transactionLogService;
    private final FraudAlertService fraudAlertService;

    public FraudDetection(TransactionProducer transactionProducer, com.fraud_detection.Fraud_Management.service.TransactionLogService transactionLogService, FraudAlertService fraudAlertService) {
        this.transactionProducer = transactionProducer;
        this.transactionLogService = transactionLogService;
        this.fraudAlertService = fraudAlertService;
    }

    @PostMapping("/check")
    public TransactionResult checkTransaction(@RequestBody TransactionDTO transaction) {
        // 1. Evaluate transaction inside the producer
        TransactionResult result = transactionProducer.processTransaction(transaction); // now returns result

        // 2. Save to transaction log (always)
        transactionLogService.createTransactionLog(
                transaction.getTransactionId(),
                transaction.getAccNoFrom(),
                transaction.getAccNoTo(),
                transaction.getAmount(),
                transaction.getTransactionType(),
                String.valueOf(result.getStatus()),
                result.getReason()
        );

        // 3. If FRAUD, log to fraud alert table
        if (result.getStatus() == com.fraud_detection.Fraud_Management.ruleengine.TransactionStatus.FRAUD) {
            fraudAlertService.createFraudAlert(
                    transaction.getTransactionId(),
                    transaction.getAccNoFrom(),
                    result.getReason()
            );
        }

        // 4. Done – no need to send notification here; handled by producer
        return result;
    }
}