package com.fraud_detection.notification.kafka;

import com.fraud_detection.notification.response.Fraud1KafkaMessage;
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
public class FraudConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(id= "fraud1-consumer-sync",
            topics= "#{'${kafka.fraud1.topic}'.split(',')}", containerFactory = "fraud1KafkaListenerFactory", clientIdPrefix = "fraud1-consumer-sync", idIsGroup = false)
    public void processRecords(@NonNull List<ConsumerRecord<String, Fraud1KafkaMessage>> data, Acknowledgment acknowledgment, @NonNull Consumer<?, ?> consumer){

        System.out.println("Processing messages from Topic...");
        try{
            for(ConsumerRecord<String, Fraud1KafkaMessage> response: data){
                notificationService.processFraud1Notification(response.value());


            }
            acknowledgment.acknowledge();
            consumer.commitAsync();
        }
        catch(Exception e){
            System.out.println("Error in Processing message from kafka: "+e.getMessage());
        }

    }
}
