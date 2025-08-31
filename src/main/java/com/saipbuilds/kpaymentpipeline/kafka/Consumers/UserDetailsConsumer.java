package com.saipbuilds.kpaymentpipeline.kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserDetailsConsumer {

    @KafkaListener(
            topics = "${kafka.topic.payment-received}",
            groupId = "payment-verif-group",
            containerFactory = "paymentKafkaListenerFactory"
    )
    public void consumePayment(PaymentReceivedEvent event) {
        log.info(" S1 Consumed PaymentReceivedEvent: {}", event);

    }
}

