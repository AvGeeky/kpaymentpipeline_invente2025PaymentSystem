package com.saipbuilds.kpaymentpipeline.kafka.Reconcillation;

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
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ReconciliationStream {

    private final ReconciliationService reconciliationService;

    @Value("${kafka.topic.payment-received}")
    private String paymentReceivedTopic;

    @Value("${kafka.topic.email-receipt}")
    private String emailReceiptTopic;

    @Value("${kafka.topic.payment-reconciled}")
    private String paymentReconciledTopic;

    @Bean
    public KStream<String, PaymentReceivedEvent> reconciliationPipeline(StreamsBuilder builder) {

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

        // ---- Re-key both streams by emailID ----
        KTable<String, PaymentReceivedEvent> paymentsByEmail = paymentStream
                .selectKey((k, v) -> v.getEmailID())
                .toTable();

        KTable<String, ReceiptReceivedEvent> receiptsByEmail = receiptStream
                .selectKey((k, v) -> v.getEmailID())
                .toTable();

        // ---- Join streams (by emailID) → ReconciledEvent ----
        KTable<String, ReconciledEvent> reconciled = paymentsByEmail.join(
                receiptsByEmail,
                (payment, receipt) -> {
                    log.info("Reconciling for email={} paymentId={}",
                            payment.getEmailID(), receipt.getPaymentID());

                    // Build ReconciledEvent
                    ReconciledEvent event = new ReconciledEvent(
                            payment.getEmailID(),
                            receipt.getPaymentID(),
                            payment.getName(),
                            receipt.getPhoneNumber(),
                            receipt.getMethod(),
                            receipt.getPaidOn(),
                            true,
                            receipt.getAmount(),
                            payment.getCreatedAt(),
                            payment.getEventBookingDetails()
                    );

                    // Persist into DB
                    reconciliationService.handleReconciliation(event);

                    return event;
                }
        );

        // ---- Produce ReconciledEvent to Kafka ----
        reconciled.toStream().to(
                paymentReconciledTopic,
                Produced.with(Serdes.String(), new JsonSerde<>(ReconciledEvent.class))
        );

        log.info("Reconciliation pipeline initialized: {} + {} → {}",
                paymentReceivedTopic, emailReceiptTopic, paymentReconciledTopic);

        return paymentStream;
    }
}
