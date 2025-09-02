package com.saipbuilds.kpaymentpipeline.Kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.HackathonPaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Service.PaymentDatabaseServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HackathonPaymentConsumer {
    private final PaymentDatabaseServices paymentDatabaseServices;

    @Autowired
    HackathonPaymentConsumer(PaymentDatabaseServices paymentDatabaseServices) {
        this.paymentDatabaseServices = paymentDatabaseServices;
    }
    @KafkaListener(
            topics = "${kafka.topic.hackathon-payment_received}",
            groupId = "receipt-verif-group",
            containerFactory = "hackPaymentKafkaListenerFactory"
    )
    public void consumeHackPaymentReceived(HackathonPaymentReceivedEvent event) {
        log.info(" S1 Produced Hackathon Payment Received Event: {}", event.getTeam_name());
        paymentDatabaseServices.insertHackDetails(event);
        // Insert user details into DB

    }
}
