package com.saipbuilds.kpaymentpipeline.Controller;

import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipt")
@RequiredArgsConstructor
@Slf4j
public class ReceiptController {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.email-receipt}")
    private String receiptTopic;

    @PostMapping
    public String publishReceipt(@RequestBody ReceiptReceivedEvent event) {
        log.info("📥 Received Receipt POST: {}", event);
        kafkaTemplate.send(receiptTopic, event.getEmailID(), event);
        return "✅ Receipt event produced to Kafka";
    }
}

