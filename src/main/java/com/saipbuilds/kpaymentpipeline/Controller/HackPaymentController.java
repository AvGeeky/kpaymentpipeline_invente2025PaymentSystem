package com.saipbuilds.kpaymentpipeline.Controller;

import com.saipbuilds.kpaymentpipeline.DTO.HackathonPaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Kafka.Producers.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hackathon-payment")
@RequiredArgsConstructor
@Slf4j
public class HackPaymentController {

    private final EventProducer eventProducer;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping
    public ResponseEntity<?> publishHackPayment(@RequestBody HackathonPaymentReceivedEvent event) {
        try {
            if (event.getLeader_email() == null || event.getPaymentID() == null) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse("Missing required fields", 400)
                );
            }

            eventProducer.sendHackathonPaymentReceivedTopic(event);
            log.info("Received Hackathon Payment POST: {}", event);

            return ResponseEntity.status(201).body(
                    new ApiResponse("Hackathon payment event produced to Kafka", 201)
            );
        } catch (Exception e) {
            log.error("Error producing hackathon payment event", e);
            return ResponseEntity.status(500).body(
                    new ApiResponse(" Failed to process hackathon payment event", 500)
            );
        }
    }
}
