package com.saipbuilds.kpaymentpipeline.Kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic.payment-received}")
    private String paymentReceivedTopic;

    @Value("${kafka.topic.email-receipt}")
    private String emailDetectedTopic;

    @Value("${kafka.topic.payment-reconciled}")
    private String paymentReconciledTopic;

    @Value("${kafka.topic.hackathon-payment_received}")
    private String hackathonPaymentReceivedTopic;

    @Value("${kafka.topic.hackathon-payment_reconciled}")
    private String hackathonPaymentReconciledTopic;

    private static final int NUM_PARTITIONS = 3;
    // ⚠️ In production, set this to >=2 (requires multiple brokers)
    private static final short REPLICATION_FACTOR = 1;

    // --- Utility constants ---
    private static final String RETENTION_7_DAYS = String.valueOf(7L * 24 * 60 * 60 * 1000);

    @Bean
    public NewTopic paymentReceivedTopic() {
        return TopicBuilder.name(paymentReceivedTopic)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.RETENTION_MS_CONFIG, RETENTION_7_DAYS) // keep events for 7 days
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }

    @Bean
    public NewTopic emailPaymentDetectedTopic() {
        return TopicBuilder.name(emailDetectedTopic)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.RETENTION_MS_CONFIG, RETENTION_7_DAYS) // keep events for 7 days
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }

    @Bean
    public NewTopic paymentReconciledTopic() {
        // Persistent: log compaction keeps latest state per key
        return TopicBuilder.name(paymentReconciledTopic)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .config(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.5")
                .config(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(24 * 60 * 60 * 1000)) // daily segments
                .build();
    }

    @Bean
    public NewTopic hackathonPaymentReceivedTopic() {
        return TopicBuilder.name(hackathonPaymentReceivedTopic)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.RETENTION_MS_CONFIG, RETENTION_7_DAYS)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .build();
    }

    @Bean
    public NewTopic hackathonPaymentReconciledTopic() {
        return TopicBuilder.name(hackathonPaymentReconciledTopic)
                .partitions(NUM_PARTITIONS)
                .replicas(REPLICATION_FACTOR)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT)
                .config(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.5")
                .config(TopicConfig.SEGMENT_MS_CONFIG, String.valueOf(24 * 60 * 60 * 1000))
                .build();
    }
}
