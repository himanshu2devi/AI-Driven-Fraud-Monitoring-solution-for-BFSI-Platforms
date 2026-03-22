package com.fraud_detection.Fraud_Management.Kafka;

import com.fraud_detection.Fraud_Management.DTO.AlertDTO;
import com.fraud_detection.Fraud_Management.DTO.NotificationDTO;
import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRuleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fraud_detection.Fraud_Management.Kafka.KafkaTopics.*;

@Service
public class TransactionProducer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProducer.class);

    private final TransactionRuleEngine ruleEngine;
    private final KafkaTemplate<String, TransactionDTO> kafkaTemplate;
    private final KafkaTemplate<String, AlertDTO> alertKafkaTemplate;
    private final KafkaTemplate<String, NotificationDTO> notificationKafkaTemplate;

    public TransactionProducer(TransactionRuleEngine ruleEngine, KafkaTemplate<String, TransactionDTO> kafkaTemplate, KafkaTemplate<String, AlertDTO> alertKafkaTemplate, KafkaTemplate<String, NotificationDTO> notificationKafkaTemplate) {
        this.ruleEngine = ruleEngine;
        this.kafkaTemplate = kafkaTemplate;
        this.alertKafkaTemplate = alertKafkaTemplate;
        this.notificationKafkaTemplate = notificationKafkaTemplate;
    }

    public TransactionResult processTransaction(TransactionDTO transaction) {
        if (transaction.getTransactionType() == null) {
            logger.warn("Received transaction with null transactionType: {}", transaction);
            return new TransactionResult(null, "Transaction type is null");
        }

        logger.info("Processing transaction: {}", transaction);

        // Evaluate the transaction (calling rule engine here)
        TransactionResult result = ruleEngine.evaluateTransaction(transaction);

        if (result == null || result.getStatus() == null) {
            logger.error("TransactionResult or status is null for transaction: {}", transaction);
            return new TransactionResult(null, "Transaction evaluation failed");
        }

        // Always send notification for each txn
        sendNotification(transaction, result);
        logger.info("Transaction evaluation result: {}", result.getStatus());

        // Sending to topics based on result
        switch (result.getStatus()) {
            case VALID:
                kafkaTemplate.send(VALID_TXN_TOPIC, transaction.getTransactionId(), transaction);
                logger.info("Transaction is VALID: {}", transaction.getTransactionId());
                break;

            case FRAUD:
                kafkaTemplate.send(ROLLBACK_TXN_TOPIC, transaction.getTransactionId(), transaction);
                // Create fraud alert only if the result contains valid reason and transactionId
                if (result.getReason() != null && transaction.getTransactionId() != null) {
                    AlertDTO fraudAlert = new AlertDTO(
                            transaction.getTransactionId(),
                            result.getReason(),
                            transaction.getAccNoFrom()
                    );
                    alertKafkaTemplate.send(ALERT_TOPIC, fraudAlert);
                    logger.info("FRAUD transaction! Rolled back and alert sent.");
                } else {
                    logger.error("Fraud alert data missing: Transaction ID or reason is null");
                }
                break;

            case ALERT:
                kafkaTemplate.send(VALID_TXN_TOPIC, transaction.getTransactionId(), transaction);
                // Create alert only if the result contains valid reason and transactionId
                if (result.getReason() != null && transaction.getTransactionId() != null) {
                    AlertDTO alert = new AlertDTO(
                            transaction.getTransactionId(),
                            result.getReason(),
                            transaction.getAccNoFrom()
                    );
                    alertKafkaTemplate.send(ALERT_TOPIC, alert);
                    logger.info("ALERT-worthy transaction. Proceeded and alert sent.");
                } else {
                    logger.error("Alert data missing: Transaction ID or reason is null");
                }
                break;

            default:
                logger.error("Unknown transaction status: {}", result.getStatus());
        }

        return result; // Ensure you're returning the result here
    }

    private void sendNotification(TransactionDTO transaction, TransactionResult result) {
        NotificationDTO notification = new NotificationDTO(
                transaction.getUserId(),
                transaction.getAccNoFrom(),
                result.getStatus().toString()
        );

        notificationKafkaTemplate.send(NOTIFICATION_TOPIC, notification);
        logger.info("Notification sent for transaction ID: {}", transaction.getTransactionId());
    }
}