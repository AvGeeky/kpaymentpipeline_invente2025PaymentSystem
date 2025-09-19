package com.saipbuilds.kpaymentpipeline.Controller;

import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.ReceiptMapper;
import com.saipbuilds.kpaymentpipeline.Kafka.Producers.EventProducer;
import com.saipbuilds.kpaymentpipeline.Service.PaymentDatabaseServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/restricted/receipt")
@RequiredArgsConstructor
@Slf4j
public class ReceiptController {
    @Autowired
    EventProducer eventProducer;
    private final PaymentDatabaseServices paymentDatabaseServices;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.email-receipt}")
    private String receiptTopic;

    @PostMapping
    public ResponseEntity<?> publishReceipt(@RequestBody ReceiptReceivedEvent event) {
        try {
            if (event.getEmailID() == null || event.getPaymentID() == null) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse("Missing required fields", 400)
                );
            }

            log.info("Received Receipt POST: {}", event);

            paymentDatabaseServices.insertReceipt(event);
            eventProducer.sendReceiptEvent(event);

            return ResponseEntity.status(201).body(
                    new ApiResponse(" Receipt event produced to Kafka", 201)
            );
        } catch (Exception e) {
            log.error("Error producing receipt event", e);
            return ResponseEntity.status(500).body(
                    new ApiResponse(" Failed to process receipt event", 500)
            );
        }
    }
}
