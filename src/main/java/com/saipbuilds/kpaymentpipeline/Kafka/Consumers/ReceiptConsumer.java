package com.saipbuilds.kpaymentpipeline.Kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Service.PaymentDatabaseServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReceiptConsumer {
    private final PaymentDatabaseServices paymentDatabaseServices;


    @Autowired
    ReceiptConsumer(PaymentDatabaseServices paymentDatabaseServices) {
        this.paymentDatabaseServices = paymentDatabaseServices;
    }
    @KafkaListener(
            topics = "${kafka.topic.email-receipt}",
            groupId = "receipt-verif-group",
            containerFactory = "receiptKafkaListenerFactory"
    )
    public void consumeReceipt(ReceiptReceivedEvent event) {
        log.info(" S2 Consumed ReceiptReceivedEvent: {}", event);
        paymentDatabaseServices.insertReceipt(event);

    }
}
