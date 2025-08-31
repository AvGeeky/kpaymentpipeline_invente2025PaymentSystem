package com.saipbuilds.kpaymentpipeline.kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentConsumer {

    @KafkaListener(
            topics = "${kafka.topic.email-receipt}",
            groupId = "receipt-verif-group",
            containerFactory = "receiptKafkaListenerFactory"
    )
    public void consumeReceipt(ReceiptReceivedEvent event) {
        log.info(" S2 Consumed ReceiptReceivedEvent: {}", event);
    }
}
