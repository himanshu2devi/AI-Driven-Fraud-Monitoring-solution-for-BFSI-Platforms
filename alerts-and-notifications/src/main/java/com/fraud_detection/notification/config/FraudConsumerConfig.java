package com.fraud_detection.notification.config;

import com.fraud_detection.notification.response.Fraud1KafkaMessage;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class FraudConsumerConfig {

    @Autowired
    private  MeterRegistry meterRegistry;

    @Value("${kafka.fraud1.servers}")
    private String kafkaBootstrapServers;

    @Value("${kafka.fraud1.consumerGroup}")
    private String fraudKafkaConsumerGroup;

    @Value("${kafka.fraud1.maxPollRecords}")
    private String fraudKafkaMaxPollRecords;

    @Value("${kafka.fraud1.maxPollIntervals}")
    private String fraudKafkaMaxPollIntervalMs;

    @Value("${kafka.fraud1.sessionTimeout}")
    private String fraudKafkaSessionTimeoutMs;

    @Value("${kafka.fraud1.concurrency}")
    private String fraudIndexingConcurrency;

    @Value("${kafka.fraud1.idleEventInterval}")
    private String fraudIdleEventIntervalMs;


    private Map<String, Object> fraud1ConsumerConfigs()
    {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, fraudKafkaConsumerGroup);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, Collections.singletonList(CooperativeStickyAssignor.class));
        return props;
    }


    private ConsumerFactory<String, Fraud1KafkaMessage> fraud1ConsumerFactory()
    {
        Map<String, Object> transactionConsumerConfigMap = fraud1ConsumerConfigs();
        transactionConsumerConfigMap.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, fraudKafkaMaxPollRecords);
        transactionConsumerConfigMap.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, fraudKafkaMaxPollIntervalMs);
        transactionConsumerConfigMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, fraudKafkaSessionTimeoutMs);

        DefaultKafkaConsumerFactory<String, Fraud1KafkaMessage> consumerFactory = new DefaultKafkaConsumerFactory<>(transactionConsumerConfigMap,
                new StringDeserializer(),
                new JsonDeserializer<>(Fraud1KafkaMessage.class,false));
        consumerFactory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return consumerFactory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Fraud1KafkaMessage> fraud1KafkaListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Fraud1KafkaMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fraud1ConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(Integer.parseInt(fraudIndexingConcurrency));
        factory.getContainerProperties().setIdleEventInterval(Long.valueOf(fraudIdleEventIntervalMs));
        factory.setBatchListener(Boolean.TRUE);
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        return factory;
    }
}
