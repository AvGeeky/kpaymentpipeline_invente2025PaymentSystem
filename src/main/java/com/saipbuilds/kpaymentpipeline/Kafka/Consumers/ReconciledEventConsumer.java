package com.saipbuilds.kpaymentpipeline.Kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReconciledEventConsumer {

    @KafkaListener(
            topics = "${kafka.topic.payment-reconciled}",
            groupId = "ticketing-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeReconciledEvent(ReconciledEvent event) {
        log.info(" S3 Consumed ReconciledEvent: email={} paymentId={} reconciled={}",
                event.getEmailID(), event.getPaymentID(), event.isReconciled());

        // 🔹 Next steps:
        // 1. Update pass table → set ticketIssued=true
        // 2. Trigger ticket generation (PDF / QR code / etc.)
        // 3. Send ticket via email to user
    }
}
