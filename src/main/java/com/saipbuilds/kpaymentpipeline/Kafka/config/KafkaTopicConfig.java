package com.saipbuilds.kpaymentpipeline.Kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;



@Configuration
public class KafkaTopicConfig {

    // It's a good practice to define these in application.yml
    @Value("${kafka.topic.payment-received}")
    private String paymentReceivedTopic;

    @Value("${kafka.topic.email-receipt}")
    private String emailDetectedTopic;

    @Value("${kafka.topic.payment-reconciled}")
    private String paymentReconciledTopic;

    private static final int NUM_PARTITIONS = 3;
    private static final short REPLICATION_FACTOR = 1;

    @Bean
    public NewTopic paymentReceivedTopic() {
        // Using TopicBuilder is a more modern and readable way
        return TopicBuilder.name(paymentReceivedTopic)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(1 * 24 * 60 * 60 * 1000)) // 1 days
                .build();
    }

    @Bean
    public NewTopic emailPaymentDetectedTopic() {
        return TopicBuilder.name(emailDetectedTopic)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(1 * 24 * 60 * 60 * 1000)) // 1 days
                .build();
    }

    @Bean
    public NewTopic paymentReconciledTopic() {
        // Use log compaction to only keep the latest status for each payment key.
        return TopicBuilder.name(paymentReconciledTopic)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }
}