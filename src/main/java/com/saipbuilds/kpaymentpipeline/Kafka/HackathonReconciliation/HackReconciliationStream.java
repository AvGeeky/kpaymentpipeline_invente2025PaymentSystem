package com.saipbuilds.kpaymentpipeline.Kafka.HackathonReconciliation;

import com.saipbuilds.kpaymentpipeline.DTO.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class HackReconciliationStream {

    private final HackDuringReconciliationService hackDuringReconciliationService;

    @Value("${kafka.topic.hackathon-payment_received}")
    private String paymentReceivedTopic;

    @Value("${kafka.topic.email-receipt}")
    private String emailReceiptTopic;

    @Value("${kafka.topic.hackathon-payment_reconciled}")
    private String paymentReconciledTopic;

    @Bean(name = "hackathonReconciliationPipeline")
    public KStream<String, HackathonPaymentReceivedEvent> hackReconciliationPipeline(StreamsBuilder builder) {

        // ---- Payment Stream ----
        KStream<String, HackathonPaymentReceivedEvent> paymentStream = builder.stream(
                paymentReceivedTopic,
                Consumed.with(Serdes.String(), new JsonSerde<>(HackathonPaymentReceivedEvent.class))
        );

        // ---- Receipt Stream ----
        KStream<String, ReceiptReceivedEvent> receiptStream = builder.stream(
                emailReceiptTopic,
                Consumed.with(Serdes.String(), new JsonSerde<>(ReceiptReceivedEvent.class))
        );

        // ---- Re-key both streams by paymentID ----
        KTable<String, HackathonPaymentReceivedEvent> paymentsByPaymentId = paymentStream
                .selectKey((k, v) -> v.getPaymentID())   // Only by PaymentID
                .toTable();

        KTable<String, ReceiptReceivedEvent> receiptsByPaymentId = receiptStream
                .selectKey((k, v) -> v.getPaymentID())   //  Only by PaymentID
                .toTable();

        // ---- Join streams (by paymentID) → ReconciledEvent ----
        KTable<String, HackathonReconciledEvent> reconciled = paymentsByPaymentId.join(
                receiptsByPaymentId,
                (payment, receipt) -> {
                    log.info("Reconciling Hack paymentId={} for email={}",
                            payment.getPaymentID(), payment.getLeader_email());

                    HackathonReconciledEvent event = HackathonReconciledEvent.builder()
                            .leader_email(payment.getLeader_email())
                            .team_name(payment.getTeam_name())
                            .track(payment.getTrack())
                            .paymentID(payment.getPaymentID())
                            .createdAt(payment.getCreatedAt())
                            .member_emails(payment.getMember_emails())
                            .member_names(payment.getMember_names())
                            .member_institutions(payment.getMember_institutions())
                            .member_phones(payment.getMember_phones())
                            .emailID(receipt.getEmailID())
                            .paidOn(receipt.getPaidOn())
                            .method(receipt.getMethod())
                            .amount(receipt.getAmount())
                            .phoneNumber(receipt.getPhoneNumber())
                            .reconciled(true)
                            .member_gender(payment.getMember_gender())
                            .member_department((payment.getMember_department()))
                            .member_yearofstudy((payment.getMember_yearofstudy()))
                            .build();

                    // Persist into DB
                    try {
                        if (!payment.getPaymentID().equals(receipt.getPaymentID())){
                            log.error(" PaymentID mismatch during reconciliation paymentId={} receiptPaymentId={}",
                                    payment.getPaymentID(), receipt.getPaymentID());
                            return null;
                        }
                        boolean b = hackDuringReconciliationService.handleReconciliation(event);
                        return b ? event : null;
                    } catch (Exception e) {
                        log.error(" error while reconciling paymentId={}", payment.getPaymentID(), e);
                        return null; // optionally skip on unexpected errors
                    }
                }
        );

// ---- Only produce non-null events ----
        reconciled.toStream()
                .filter((k, v) -> v != null)
                .to(paymentReconciledTopic,
                        Produced.with(Serdes.String(), new JsonSerde<>(HackathonReconciledEvent.class)));

        log.info("Hack Reconciliation pipeline initialized: {} + {} → {}",
                paymentReceivedTopic, emailReceiptTopic, paymentReconciledTopic);

        return paymentStream;
    }
}
