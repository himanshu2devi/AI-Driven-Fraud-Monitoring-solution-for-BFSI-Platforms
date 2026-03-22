//package com.fraud_detection.notification.kafka;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import lombok.AllArgsConstructor;
//import lombok.extern.log4j.Log4j2;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//@Component
//@Log4j2
//@AllArgsConstructor
//public class Notification_consumer {
//    private final ObjectMapper objectMapper;
//    private final ExecutorService executorService;
//    private final String notificationConsumerTopic;
//
//
//    @Autowired
//    public Notification_consumer(ObjectMapper objectMapper, @Value("${application.kafka.process-message-threadpool-size:25}") String threadPoolSize,
//                                 @Value("${application.kafka.notification-consumer-topic}") String notificationConsumerTopic) {
//        this.objectMapper = objectMapper.copy();
//        this.executorService = Executors.newFixedThreadPool(Integer.parseInt(threadPoolSize));
//        this.notificationConsumerTopic = notificationConsumerTopic;
//        objectMapper.registerModule(new JavaTimeModule());
//    }
//
//    @KafkaListener(topics = {"${application.kafka.notification-consumer-topic}",}, groupId = "${application.kafka.consumer.group-id}")
//    public void consume(@Payload String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.RECEIVED_PARTITION) String partition, @Header(KafkaHeaders.OFFSET) String offset) {
//        log.info("Message received from topic={}, partitionId={}, offsetId={}", () -> topic, () -> partition, () -> offset);
//        log.debug("Payload: {}", payload);
//        // we need to add the logic to send notification based on the message structure received by consumer.
//
//    }
//}
//
//
