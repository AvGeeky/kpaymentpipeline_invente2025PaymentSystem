package com.saipbuilds.kpaymentpipeline.Controller;


import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.payment-received}")
    private String paymentTopic;

    @PostMapping
    public String publishPayment(@RequestBody PaymentReceivedEvent event) {
        log.info("📥 Received Payment POST: {}", event);
        kafkaTemplate.send(paymentTopic, event.getEmailID(), event);
        return "✅ Payment event produced to Kafka";
    }
}
