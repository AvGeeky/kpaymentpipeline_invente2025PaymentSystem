package com.saipbuilds.kpaymentpipeline.kafka.Reconcillation;

import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReconciliationService {

    public void handleReconciliation(ReconciledEvent event) {
        // Example DB operations (via MyBatis mapper / JPA):
        // 1. Update users SET status_paid=true WHERE email = event.getEmailID()
        // 2. Insert INTO receipts (...)
        // 3. Insert INTO pass (ticketIssued=false)
        // 4. Insert INTO slots (...)

        log.info(" S3 Reconciled email={} paymentId={} reconciled={}",
                event.getEmailID(), event.getPaymentID(), event.isReconciled());
    }
}
