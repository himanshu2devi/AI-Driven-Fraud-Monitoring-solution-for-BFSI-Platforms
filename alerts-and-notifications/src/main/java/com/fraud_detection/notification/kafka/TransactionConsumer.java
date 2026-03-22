package com.fraud_detection.notification.kafka;

import com.fraud_detection.notification.response.TransactionKafkaMessage;
import com.fraud_detection.notification.service.NotificationService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableKafka
@RequiredArgsConstructor
@Component
public class TransactionConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(id= "transaction-consumer-sync",
            topics= "#{'${kafka.transaction.topic}'.split(',')}", containerFactory = "transactionsKafkaListenerFactory", clientIdPrefix = "transaction-consumer-sync", idIsGroup = false)
    public void processRecords(@NonNull List<ConsumerRecord<String, TransactionKafkaMessage>> data, Acknowledgment acknowledgment, @NonNull Consumer<?, ?> consumer){

        System.out.println("Processing messages from Transaction Topic...");
        try{
            for(ConsumerRecord<String, TransactionKafkaMessage> response: data){
                notificationService.sendTransactionNotification(response.value());

            }
            acknowledgment.acknowledge();
            consumer.commitAsync();
        }
        catch(Exception e){
            System.out.println("Error in Processing message from kafka: "+e.getMessage());
        }

    }
}
