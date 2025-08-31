package com.saipbuilds.kpaymentpipeline.Kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.PassMapper;
import com.saipbuilds.kpaymentpipeline.TicketSender.emailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ReconciledEventConsumer {
    private final emailUtil emailSender;
    private final PassMapper passMapper;
    @Autowired
    public ReconciledEventConsumer(emailUtil emailSender, PassMapper passMapper) {
        this.emailSender = emailSender;
        this.passMapper = passMapper;
    }

    @KafkaListener(
            topics = "${kafka.topic.payment-reconciled}",
            groupId = "ticketing-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeReconciledEvent(ReconciledEvent event) {
        log.info(" S3 Consumed ReconciledEvent: email={} paymentId={} reconciled={}",
                event.getEmailID(), event.getPaymentID(), event.isReconciled());
        try {
            emailSender.sendTicketPurchaseMail(event.getEmailID(), event);
            log.info("Ticket email queued for sending to {}", event.getEmailID());
        } catch (Exception e) {
            log.error("Failed to queue ticket email for paymentID={}", event.getPaymentID(), e);
        }

        List<String> ticketUuids = passMapper.findUuidsByPaymentId(event.getPaymentID());
        for(String s: ticketUuids){
            log.info(" S4 Ticket generated with UUID: {}", s);
            passMapper.markTicketIssued(s);
        }

        // 🔹 Next steps:
        // 1. Update pass table → set ticketIssued=true
        // 2. Trigger ticket generation (PDF / QR code / etc.)
        // 3. Send ticket via email to user
    }
}
