package com.fraud_detection.notification.kafka;


import com.fraud_detection.notification.response.FraudKafkaMessage;
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
public class FraudDetectionConsumer {

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(id= "fraud-consumer-sync",
    topics= "#{'${kafka.fraud.topic}'.split(',')}", containerFactory = "fraudKafkaListenerFactory", clientIdPrefix = "fraud-consumer-sync", idIsGroup = false)
    public void processRecords(@NonNull List<ConsumerRecord<String, FraudKafkaMessage>> data, Acknowledgment acknowledgment, @NonNull Consumer<?, ?> consumer){

        System.out.println("Processing messages from Topic...");
        try{
            for(ConsumerRecord<String, FraudKafkaMessage> response: data){
                notificationService.processFraudNotification(response.value());


            }
            acknowledgment.acknowledge();
            consumer.commitAsync();
        }
        catch(Exception e){
            System.out.println("Error in Processing message from kafka: "+e.getMessage());
        }

    }
}
