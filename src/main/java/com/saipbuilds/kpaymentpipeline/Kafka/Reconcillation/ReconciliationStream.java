package com.saipbuilds.kpaymentpipeline.Kafka.Reconcillation;

import com.saipbuilds.kpaymentpipeline.DTO.PaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReceiptReceivedEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ReconciliationStream {

    private final DuringReconciliationService duringReconciliationService;

    @Value("${kafka.topic.payment-received}")
    private String paymentReceivedTopic;

    @Value("${kafka.topic.email-receipt}")
    private String emailReceiptTopic;

    @Value("${kafka.topic.payment-reconciled}")
    private String paymentReconciledTopic;

    @Bean
    public KStream<String, PaymentReceivedEvent> normalReconciliationPipeline(StreamsBuilder builder) {

        // ---- Payment Stream ----
        KStream<String, PaymentReceivedEvent> paymentStream = builder.stream(
                paymentReceivedTopic,
                Consumed.with(Serdes.String(), new JsonSerde<>(PaymentReceivedEvent.class))
        );

        // ---- Receipt Stream ----
        KStream<String, ReceiptReceivedEvent> receiptStream = builder.stream(
                emailReceiptTopic,
                Consumed.with(Serdes.String(), new JsonSerde<>(ReceiptReceivedEvent.class))
        );

        // ---- Re-key both streams by paymentID ----
        KTable<String, PaymentReceivedEvent> paymentsByPaymentId = paymentStream
                .selectKey((k, v) -> v.getPaymentID())   // Only by PaymentID
                .toTable();

        KTable<String, ReceiptReceivedEvent> receiptsByPaymentId = receiptStream
                .selectKey((k, v) -> v.getPaymentID())   //  Only by PaymentID
                .toTable();

        // ---- Join streams (by paymentID) → ReconciledEvent ----
        KTable<String, ReconciledEvent> reconciled = paymentsByPaymentId.join(
                receiptsByPaymentId,
                (payment, receipt) -> {
                    log.info("Reconciling paymentId={} for email={}",
                            payment.getPaymentID(), payment.getEmailID());

                    ReconciledEvent event = new ReconciledEvent(
                            payment.getEmailID(),
                            payment.getPaymentID(),
                            payment.getName(),
                            receipt.getPhoneNumber(),
                            receipt.getMethod(),
                            receipt.getPaidOn(),
                            true,
                            receipt.getAmount(),
                            payment.getCreatedAt(),
                            payment.getEventBookingDetails(),
                            payment.getType()
                    );

                    // Persist into DB
                    try {
                        if (!payment.getPaymentID().equals(receipt.getPaymentID())){
                            log.error(" PaymentID mismatch during reconciliation paymentId={} receiptPaymentId={}",
                                    payment.getPaymentID(), receipt.getPaymentID());
                            return null;
                        }
                        boolean b = duringReconciliationService.handleReconciliation(event);
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
                        Produced.with(Serdes.String(), new JsonSerde<>(ReconciledEvent.class)));

        log.info("Reconciliation pipeline initialized: {} + {} → {}",
                paymentReceivedTopic, emailReceiptTopic, paymentReconciledTopic);

        return paymentStream;
    }
}
