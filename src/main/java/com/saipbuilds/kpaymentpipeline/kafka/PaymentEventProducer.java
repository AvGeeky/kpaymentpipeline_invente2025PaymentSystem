package com.saipbuilds.kpaymentpipeline.kafka;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;



@Service
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Send PaymentReceivedEvent
    public void sendPaymentEvent(PaymentReceivedEvent event) {
        kafkaTemplate.send("payment.received", event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send PaymentReceivedEvent {}: {}", event, ex.getMessage(), ex);
                    } else {
                        log.info("➡️ Published PaymentReceivedEvent to topic {} partition {} offset {}: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event);
                    }
                });
    }

    // Send ReceiptReceivedEvent
    public void sendReceiptEvent(ReceiptReceivedEvent event) {
        kafkaTemplate.send("email.payment_detected", event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send ReceiptReceivedEvent {}: {}", event, ex.getMessage(), ex);
                    } else {
                        log.info("➡️ Published ReceiptReceivedEvent to topic {} partition {} offset {}: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                event);
                    }
                });
    }
}
