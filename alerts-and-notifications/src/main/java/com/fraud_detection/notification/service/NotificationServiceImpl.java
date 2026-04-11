package com.fraud_detection.notification.service;

import com.fraud_detection.notification.entity.TransactionEntity;
import com.fraud_detection.notification.entity.UserDetailsEntity;
import com.fraud_detection.notification.repository.NotificationRepository;
import com.fraud_detection.notification.repository.TransactionRepository;
import com.fraud_detection.notification.response.Fraud1KafkaMessage;
import com.fraud_detection.notification.response.FraudKafkaMessage;
import com.fraud_detection.notification.response.TransactionKafkaMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Autowired
    private  NotificationRepository notificationRepository;

    @Autowired
    private  TransactionRepository transactionRepository;

    @Autowired
    private JavaMailSender mailSender;


    public void sendNotification(Long userId, String message) {

        // Logic to send notification
//        "fetch email_id from db, using user_id and account_number";
        //log.info("Sending notification: {}", userId);
        // Here you would implement the actual notification sending logic
        // For example, sending an email or a push notification
        UserDetailsEntity userDetails = notificationRepository.findById(userId);
        String emailId = userDetails.getEmail();
        // Send email
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(emailId);
        mailMessage.setSubject("BANK NOTIFICATION : TRANSACTION ALERT");
        mailMessage.setText(message);

        try {
            mailSender.send(mailMessage);
          //  log.info("Email sent successfully to {}", emailId);
        } catch (Exception e) {
          //  log.error("Failed to send email to {}: {}", emailId, e.getMessage());
        }
    }

    @Override
    public void sendTransactionNotification(TransactionKafkaMessage transactionKafkaMessage) {

        UserDetailsEntity userDetailsEntity = notificationRepository.findById(transactionKafkaMessage.getUser_id());
        String emailId = userDetailsEntity.getEmail();
        // Send email
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(username);
        mailMessage.setTo(emailId);
        mailMessage.setSubject("BANK NOTIFICATION : TRANSACTION ALERT");
        mailMessage.setText("Your recent Transaction is "+transactionKafkaMessage.getStatus()+"If you have not initiated this transaction,inform bank immediately.");

        try {
            mailSender.send(mailMessage);
           // log.info("Email sent successfully to {}", emailId);
        } catch (Exception e) {
          //  log.error("Failed to send email to {}: {}", emailId, e.getMessage());
        }
    }

    @Override
    public void processFraudNotification(FraudKafkaMessage fraudKafkaMessage) {

        Optional<TransactionEntity> transactionEntity = transactionRepository.findById((fraudKafkaMessage.getTransactionId()));
        UserDetailsEntity userDetailsEntity = notificationRepository.findById(transactionEntity.get().getUserId());
        String emailId = userDetailsEntity.getEmail();
        // Send email
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(username);
        mailMessage.setTo(emailId);
        mailMessage.setSubject("ALERT Notification");
        mailMessage.setText("ALERT!! "+fraudKafkaMessage.getReason() +" for account"+fraudKafkaMessage.getAccountNo());

        try {
            mailSender.send(mailMessage);
           // log.info("Email sent successfully to {}", emailId);
        } catch (Exception e) {
           // log.error("Failed to send email to {}: {}", emailId, e.getMessage());
        }
    }

    @Override
    public void processFraud1Notification(Fraud1KafkaMessage fraud1KafkaMessage) {
        UserDetailsEntity userDetailsEntity = notificationRepository.findById(Long.valueOf(fraud1KafkaMessage.getUserId()));
        String emailId = userDetailsEntity.getEmail();
        // Send email
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(username);
        mailMessage.setTo(emailId);
        mailMessage.setSubject("ALERT Notification");
        mailMessage.setText("ALERT!! "+fraud1KafkaMessage.getTransactionStatus() +" for account"+fraud1KafkaMessage.getAccountNo());


        try {
            mailSender.send(mailMessage);
          ///  log.info("Email sent successfully to {}", emailId);
        } catch (Exception e) {
           // log.error("Failed to send email to {}: {}", emailId, e.getMessage());
        }
    }
}


