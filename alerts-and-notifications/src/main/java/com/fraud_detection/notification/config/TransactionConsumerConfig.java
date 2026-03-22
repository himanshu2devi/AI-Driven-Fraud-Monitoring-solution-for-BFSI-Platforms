package com.fraud_detection.notification.config;

import com.fraud_detection.notification.response.TransactionKafkaMessage;
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
public class TransactionConsumerConfig {

    @Autowired
    private  MeterRegistry meterRegistry;

    @Value("${kafka.transaction.servers}")
    private String kafkaBootstrapServers;

    @Value("${kafka.transaction.consumerGroup}")
    private String transactionKafkaConsumerGroup;

    @Value("${kafka.transaction.maxPollRecords}")
    private String transactionKafkaMaxPollRecords;

    @Value("${kafka.transaction.maxPollIntervals}")
    private String transactionKafkaMaxPollIntervalMs;

    @Value("${kafka.transaction.sessionTimeout}")
    private String transactionKafkaSessionTimeoutMs;

    @Value("${kafka.transaction.concurrency}")
    private String transactionIndexingConcurrency;

    @Value("${kafka.transaction.idleEventInterval}")
    private String transactionIdleEventIntervalMs;

    private Map<String, Object> transactionConsumerConfigs()
    {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, transactionKafkaConsumerGroup);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, Collections.singletonList(CooperativeStickyAssignor.class));
        return props;
    }


    private ConsumerFactory<String, TransactionKafkaMessage> transactionsConsumerFactory()
    {
        Map<String, Object> transactionConsumerConfigMap = transactionConsumerConfigs();
        transactionConsumerConfigMap.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, transactionKafkaMaxPollRecords);
        transactionConsumerConfigMap.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, transactionKafkaMaxPollIntervalMs);
        transactionConsumerConfigMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, transactionKafkaSessionTimeoutMs);

        DefaultKafkaConsumerFactory<String, TransactionKafkaMessage> consumerFactory = new DefaultKafkaConsumerFactory<>(transactionConsumerConfigMap,
                new StringDeserializer(),
                new JsonDeserializer<>(TransactionKafkaMessage.class,false));
        consumerFactory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return consumerFactory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionKafkaMessage> transactionsKafkaListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TransactionKafkaMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionsConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setConcurrency(Integer.parseInt(transactionIndexingConcurrency));
        factory.getContainerProperties().setIdleEventInterval(Long.valueOf(transactionIdleEventIntervalMs));
        factory.setBatchListener(Boolean.TRUE);
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        return factory;

    }

}
