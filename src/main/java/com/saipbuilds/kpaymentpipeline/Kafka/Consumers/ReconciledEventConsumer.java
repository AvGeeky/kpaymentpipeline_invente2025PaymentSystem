package com.saipbuilds.kpaymentpipeline.Kafka.Consumers;

import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.EventMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.PassMapper;
import com.saipbuilds.kpaymentpipeline.TicketSender.emailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReconciledEventConsumer {
    private final emailUtil emailSender;
    private final PassMapper passMapper;
    private final EventMapper eventMapper;
    String paperEventIDs = System.getenv("PAPER_PRESENTATION_EVENT_IDS");
    List<String> paperEventIDList = List.of(paperEventIDs != null ? paperEventIDs.split(",") : new String[0]);
    @Autowired
    public ReconciledEventConsumer(emailUtil emailSender, PassMapper passMapper, EventMapper eventMapper) {
        this.emailSender = emailSender;
        this.passMapper = passMapper;
        this.eventMapper = eventMapper;

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


        Map<String, String> eventIdToName = new LinkedHashMap<>();
        Map<String, String> eventIdToForm = new LinkedHashMap<>();

        for (int i = 0; i < event.getEventBookingDetails().size(); i++) {
            for (Object value : event.getEventBookingDetails().get(i).values()) {
                String eventId = String.valueOf(value);
                if (paperEventIDList.contains(eventId)) {
                    String envKey = "PAPER_PRESENTATION_GFORM_FOR_" + eventId;
                    String gformLink = System.getenv(envKey);

                    String eventName;
                    try {
                        eventName = eventMapper.findEventNameById(Integer.parseInt(eventId));
                    } catch (Exception e) {
                        eventName = "Unknown Event";
                    }

                    eventIdToName.put(eventId, eventName);
                    eventIdToForm.put(eventId, gformLink);
                }
            }
        }

        if (!eventIdToName.isEmpty()) {
            emailSender.sendPaperPublicationEmail(event.getEmailID(), eventIdToName, eventIdToForm);
            log.info("Bundled paper publication email queued for {}", event.getEmailID());
        }


        List<String> ticketUuids = passMapper.findUuidsByPaymentId(event.getPaymentID());
        for(String s: ticketUuids){
            log.info(" S4 Ticket generated with UUID: {}", s);
            passMapper.markTicketIssued(s);
        }


        // 1. Update pass table → set ticketIssued=true
        // 2. Trigger ticket generation (PDF / QR code / etc.)
        // 3. Send ticket via email to user
    }
}
