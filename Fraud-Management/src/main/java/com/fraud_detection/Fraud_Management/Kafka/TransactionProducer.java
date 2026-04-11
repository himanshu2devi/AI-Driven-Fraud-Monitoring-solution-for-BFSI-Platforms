package com.fraud_detection.Fraud_Management.Kafka;

import com.fraud_detection.Fraud_Management.AI.FraudScoringService;
import com.fraud_detection.Fraud_Management.DTO.AlertDTO;
import com.fraud_detection.Fraud_Management.DTO.NotificationDTO;
import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import com.fraud_detection.Fraud_Management.ruleengine.ResultHolder;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionResult;
import com.fraud_detection.Fraud_Management.ruleengine.TransactionRuleEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fraud_detection.Fraud_Management.ruleengine.*;

import static com.fraud_detection.Fraud_Management.Kafka.KafkaTopics.*;

@Service
public class TransactionProducer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionProducer.class);

    private final TransactionRuleEngine ruleEngine;
    private final KafkaTemplate<String, TransactionDTO> kafkaTemplate;
    private final KafkaTemplate<String, AlertDTO> alertKafkaTemplate;
    private final KafkaTemplate<String, NotificationDTO> notificationKafkaTemplate;

    private final FraudScoringService fraudScoringService;

    public TransactionProducer(
            TransactionRuleEngine ruleEngine,
            KafkaTemplate<String, TransactionDTO> kafkaTemplate,
            KafkaTemplate<String, AlertDTO> alertKafkaTemplate,
            KafkaTemplate<String, NotificationDTO> notificationKafkaTemplate,
            FraudScoringService fraudScoringService
    ) {
        this.ruleEngine = ruleEngine;
        this.kafkaTemplate = kafkaTemplate;
        this.alertKafkaTemplate = alertKafkaTemplate;
        this.notificationKafkaTemplate = notificationKafkaTemplate;
        this.fraudScoringService = fraudScoringService;
    }

    public TransactionResult processTransaction(TransactionDTO transaction) {

        if (transaction.getTransactionType() == null) {
            logger.warn("Received transaction with null transactionType: {}", transaction);
            return new TransactionResult(null, "Transaction type is null");
        }

        logger.info("Processing transaction: {}", transaction);

        // 🔥 STEP 1: Rule Engine
        ResultHolder holder = ruleEngine.evaluateTransaction(transaction);

        // 🔥 STEP 2: AI SCORING
        fraudScoringService.enrichWithAiScore(holder, transaction);

        // 🔥 STEP 3: NULL CHECK
        if (holder == null || holder.getStatus() == null) {
            logger.error("ResultHolder or status is null for transaction: {}", transaction);
            return new TransactionResult(null, "Transaction evaluation failed");
        }

        // 🔥 STEP 4: Convert to TransactionResult
        TransactionResult result = new TransactionResult(
                holder.getStatus(),
                "Risk: " + holder.getRiskLevel() +
                        " | Score: " + holder.getFinalScore() +
                        " | Reason: " + holder.getReason()
        );

        // 🔥 STEP 5: SEND NOTIFICATION
        sendNotification(transaction, result);
        logger.info("Transaction evaluation result: {}", result.getStatus());

        // 🔥 STEP 6: SEND TO KAFKA BASED ON RESULT
        switch (result.getStatus()) {

            case VALID:
                kafkaTemplate.send(VALID_TXN_TOPIC, transaction.getTransactionId(), transaction);
                logger.info("Transaction is VALID: {}", transaction.getTransactionId());
                break;

            case FRAUD:
                kafkaTemplate.send(ROLLBACK_TXN_TOPIC, transaction.getTransactionId(), transaction);

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

        return result;
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