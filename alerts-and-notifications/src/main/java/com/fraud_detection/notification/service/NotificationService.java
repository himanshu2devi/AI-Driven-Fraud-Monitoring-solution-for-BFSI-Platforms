package com.fraud_detection.notification.service;

import com.fraud_detection.notification.response.Fraud1KafkaMessage;
import com.fraud_detection.notification.response.FraudKafkaMessage;
import com.fraud_detection.notification.response.TransactionKafkaMessage;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {

    void sendTransactionNotification(TransactionKafkaMessage transactionKafkaMessage);

    void processFraudNotification(FraudKafkaMessage fraudKafkaMessage);

    void processFraud1Notification(Fraud1KafkaMessage fraud1KafkaMessage);


}
