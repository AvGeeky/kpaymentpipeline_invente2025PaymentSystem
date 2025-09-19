package com.saipbuilds.kpaymentpipeline.Controller;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Database.MongoDB.PaymentDB;
import com.saipbuilds.kpaymentpipeline.Kafka.Producers.EventProducer;
import com.saipbuilds.kpaymentpipeline.Service.PaymentDatabaseServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final EventProducer eventProducer;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentDatabaseServices paymentDatabaseServices;
    private final PaymentDB paymentDB;



    @PostMapping
    public ResponseEntity<?> publishPayment(@RequestBody PaymentReceivedEvent event) {
        try {
            if (event.getEmailID() == null || event.getPaymentID() == null) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse("Missing required fields", 400)
                );
            }

            log.info(" Received Payment POST: {}", event);
            paymentDatabaseServices.insertDetails(event);
            paymentDB.insertPayment(event);
            eventProducer.sendPaymentEvent(event);

            return ResponseEntity.status(201).body(
                    new ApiResponse("Payment event produced to Kafka", 201)
            );
        } catch (Exception e) {
            log.error("Error producing payment event", e);
            return ResponseEntity.status(500).body(
                    new ApiResponse("Failed to process payment event", 500)
            );
        }
    }
}
