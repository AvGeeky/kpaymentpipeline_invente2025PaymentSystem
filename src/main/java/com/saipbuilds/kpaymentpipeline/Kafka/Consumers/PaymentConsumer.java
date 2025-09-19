package com.saipbuilds.kpaymentpipeline.Kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Service.PaymentDatabaseServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentConsumer {
    PaymentDatabaseServices paymentDatabaseServices;
    @Autowired
    PaymentConsumer(PaymentDatabaseServices paymentDatabaseServices) {
        this.paymentDatabaseServices = paymentDatabaseServices;
    }

    @KafkaListener(
            topics = "${kafka.topic.payment-received}",
            groupId = "payment-verif-group",
            containerFactory = "paymentKafkaListenerFactory"
    )
    public void consumePayment(PaymentReceivedEvent event) {
        log.info(" S1 Consumed PaymentReceivedEvent: {}", event);
        //paymentDatabaseServices.insertDetails(event);
    }
}

