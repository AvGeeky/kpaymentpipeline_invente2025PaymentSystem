package com.saipbuilds.kpaymentpipeline.Kafka.config;
//To configure Kafka consumer to read JSON messages and convert them to Java objects

import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private Map<String, Object> baseProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    // ---- Payment Consumer ----
    @Bean
    public ConsumerFactory<String, PaymentReceivedEvent> paymentConsumerFactory() {
        JsonDeserializer<PaymentReceivedEvent> deserializer =
                new JsonDeserializer<>(PaymentReceivedEvent.class).ignoreTypeHeaders();
        deserializer.addTrustedPackages("com.saipbuilds.kpaymentpipeline.DTO");

        return new DefaultKafkaConsumerFactory<>(
                baseProps("payment-verif-group"),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentReceivedEvent> paymentKafkaListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentReceivedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentConsumerFactory());
        return factory;
    }

    // ---- Receipt Consumer ----
    @Bean
    public ConsumerFactory<String, ReceiptReceivedEvent> receiptConsumerFactory() {
        JsonDeserializer<ReceiptReceivedEvent> deserializer =
                new JsonDeserializer<>(ReceiptReceivedEvent.class).ignoreTypeHeaders();
        deserializer.addTrustedPackages("com.saipbuilds.kpaymentpipeline.DTO");

        return new DefaultKafkaConsumerFactory<>(
                baseProps("receipt-verif-group"),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReceiptReceivedEvent> receiptKafkaListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReceiptReceivedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(receiptConsumerFactory());
        return factory;
    }
}
