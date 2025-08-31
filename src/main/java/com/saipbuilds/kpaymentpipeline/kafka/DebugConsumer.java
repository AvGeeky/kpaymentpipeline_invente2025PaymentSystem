package com.saipbuilds.kpaymentpipeline.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DebugConsumer {

    private static final Logger log = LoggerFactory.getLogger(DebugConsumer.class);

    @KafkaListener(topics = "payment.received", groupId = "debug-group")
    public void listenPayment(String message) {
        log.info("Consumed from payment.received: {}", message);
    }

    @KafkaListener(topics = "email.payment_detected", groupId = "debug-group")
    public void listenReceipt(String message) {
        log.info("Consumed from email.payment_detected: {}", message);
    }
}

