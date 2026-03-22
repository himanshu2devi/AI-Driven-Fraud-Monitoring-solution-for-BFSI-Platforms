package com.fraud_detection.Fraud_Management.Service;

import com.fraud_detection.Fraud_Management.entity.FraudAlert;
import com.fraud_detection.Fraud_Management.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;

    public FraudAlertService(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    public FraudAlert createFraudAlert(String transactionId, String accountNumber, String alertMessage) {
        FraudAlert alert = new FraudAlert();
        alert.setTransactionId(transactionId);
        alert.setAccountNumber(accountNumber);
        alert.setAlertMessage(alertMessage);
        alert.setCreatedAt(LocalDateTime.now());

        return fraudAlertRepository.save(alert);
    }
}