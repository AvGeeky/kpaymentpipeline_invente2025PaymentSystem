package com.saipbuilds.kpaymentpipeline.Controller;


import com.saipbuilds.kpaymentpipeline.DTO.HackathonPaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Kafka.Producers.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hackathon-payment")
@RequiredArgsConstructor
@Slf4j
public class HackPaymentController {
    private final EventProducer eventProducer;
    private final KafkaTemplate<String, Object> kafkaTemplate;



    @PostMapping
    public String publishHackPayment(@RequestBody HackathonPaymentReceivedEvent event) {
        eventProducer.sendHackathonPaymentReceivedTopic(event);
        log.info("📥 Received hack Payment POST: {}", event);
        return "✅ hack Payment event produced to Kafka";
    }
}
