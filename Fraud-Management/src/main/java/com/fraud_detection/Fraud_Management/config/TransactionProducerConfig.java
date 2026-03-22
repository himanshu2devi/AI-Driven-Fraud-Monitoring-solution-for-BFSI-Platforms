package com.fraud_detection.Fraud_Management.config;

import com.fraud_detection.Fraud_Management.DTO.AlertDTO;
import com.fraud_detection.Fraud_Management.DTO.NotificationDTO;
import com.fraud_detection.Fraud_Management.DTO.TransactionDTO;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class TransactionProducerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    // Common Producer Configurations
    private Map<String, Object> commonProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false); // To avoid type headers
        return props;
    }

    // ProducerFactory for TransactionDTO
    @Bean
    public ProducerFactory<String, TransactionDTO> transactionDTOProducerFactory() {
        return new DefaultKafkaProducerFactory<>(commonProducerConfigs());
    }

    // KafkaTemplate for TransactionDTO
    @Bean
    public KafkaTemplate<String, TransactionDTO> transactionDTOKafkaTemplate() {
        return new KafkaTemplate<>(transactionDTOProducerFactory());
    }

    // ProducerFactory for AlertDTO
    @Bean
    public ProducerFactory<String, AlertDTO> alertDTOProducerFactory() {
        return new DefaultKafkaProducerFactory<>(commonProducerConfigs());
    }

    // KafkaTemplate for AlertDTO
    @Bean
    public KafkaTemplate<String, AlertDTO> alertDTOKafkaTemplate() {
        return new KafkaTemplate<>(alertDTOProducerFactory());
    }

    @Bean
    public ProducerFactory<String, NotificationDTO> notificationDTOProducerFactory() {
        return new DefaultKafkaProducerFactory<>(commonProducerConfigs());
    }

    @Bean
    public KafkaTemplate<String, NotificationDTO> notificationDTOKafkaTemplate() {
        return new KafkaTemplate<>(notificationDTOProducerFactory());
    }
}