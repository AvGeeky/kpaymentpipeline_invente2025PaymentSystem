package com.saipbuilds.kpaymentpipeline.Kafka.Producers;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${kafka.topic.payment-received}")
    private String PAYMENT_TOPIC;

    @Value("${kafka.topic.email-receipt}")
    private String RECEIPT_TOPIC;


    public void sendPaymentEvent(PaymentReceivedEvent event) {
        try {
            kafkaTemplate.send(PAYMENT_TOPIC, event.getEmailID(), event);
            log.info(" S1 PaymentReceivedEvent published for user [{}]", event.getEmailID());
        } catch (Exception e) {
            log.error("Failed to publish PaymentReceivedEvent for [{}]: {}", event.getEmailID(), e.getMessage(), e);
        }
    }

    public void sendReceiptEvent(ReceiptReceivedEvent event) {
        try {
            kafkaTemplate.send(RECEIPT_TOPIC, event.getPaymentID(), event);
            log.info(" S2 ReceiptReceivedEvent published for paymentID [{}]", event.getPaymentID());
        } catch (Exception e) {
            log.error("Failed to publish ReceiptReceivedEvent for [{}]: {}", event.getPaymentID(), e.getMessage(), e);
        }
    }
}

