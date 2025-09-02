package com.saipbuilds.kpaymentpipeline.Kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.HackathonReconciledEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.HackPassMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.PassMapper;
import com.saipbuilds.kpaymentpipeline.TicketSender.emailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class HackathonReconciledEventConsumer {
    private final emailUtil emailSender;
    private final PassMapper passMapper;
    private final HackPassMapper hackPassMapper;

    @Autowired
    public HackathonReconciledEventConsumer(emailUtil emailSender, PassMapper passMapper, HackPassMapper hackPassMapper) {
        this.emailSender = emailSender;
        this.passMapper = passMapper;
        this.hackPassMapper = hackPassMapper;
    }

    @KafkaListener(
            topics = "${kafka.topic.hackathon-payment_reconciled}",
            groupId = "ticketing-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeHackReconciledEvent(HackathonReconciledEvent event) {
        List<String> ticketUuids = hackPassMapper.findTeamIdByPaymentId(event.getPaymentID());
        log.info(" S3.B Consumed HackathonReconciledEvent: email={} paymentId={} reconciled={}",
                event.getEmailID(), event.getPaymentID(), event.isReconciled());
        try {
            emailSender.sendHackathonTicketMail(event.getEmailID(), event, ticketUuids);
            log.info("Ticket email queued for sending to {}", event.getEmailID());
        } catch (Exception e) {
            log.error("Failed to queue ticket email for paymentID={}", event.getPaymentID(), e);
        }
        for(String s: ticketUuids){
            log.info(" S4 Ticket generated with UUID: {}", s);
            hackPassMapper.markTicketIssued(s);
        }

        // 🔹 Next steps:
        // 1. Update pass table → set ticketIssued=true
        // 2. Trigger ticket generation (PDF / QR code / etc.)
        // 3. Send ticket via email to user
    }
}
