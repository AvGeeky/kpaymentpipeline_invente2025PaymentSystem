package com.saipbuilds.kpaymentpipeline.Kafka.HackathonReconciliation;

import com.saipbuilds.kpaymentpipeline.DTO.HackathonReconciledEvent;
import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.PassMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.ReceiptMapper;
import com.saipbuilds.kpaymentpipeline.Service.PaymentDatabaseServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class HackDuringReconciliationService {
    private final ReceiptMapper receiptMapper;
    PaymentDatabaseServices paymentDatabaseServices;

    String amountString = System.getenv("AMOUNT_PERPASS_HACK");
    int amount = Integer.parseInt(amountString);
    Integer amountPerPass = Integer.parseInt(amountString != null ? amountString : "300");
    private final PassMapper passMapper;

    public HackDuringReconciliationService(PassMapper passMapper, PaymentDatabaseServices paymentDatabaseServices, ReceiptMapper receiptMapper) {
        this.passMapper = passMapper;
        this.paymentDatabaseServices=paymentDatabaseServices;
        this.receiptMapper = receiptMapper;
    }

    public boolean handleReconciliation(HackathonReconciledEvent event) {
        if (event.getAmount()==null){
            log.warn(" Reconciliation skipped: Invalid amount for paymentID={} email={}",
                    event.getPaymentID(), event.getEmailID());
            return false;
        }
        int amountBD = (int) Double.parseDouble(event.getAmount());
        if (amountBD!=amount){
            log.warn(" Reconciliation skipped: Incorrect amount for paymentID={} email={}",
                    event.getPaymentID(), event.getEmailID());
            return false;
        }

        boolean b=receiptMapper.isPassGenerated(event.getPaymentID());
        if (b){
            log.warn(" Reconciliation skipped: Passes already generated for paymentID={} email={}",
                    event.getPaymentID(), event.getEmailID());
            return false;
        }


        receiptMapper.markPassGeneratedTrue(event.getPaymentID());


        log.info(" S3.B Reconciled email={} paymentId={} reconciled={}",
                event.getEmailID(), event.getPaymentID(), event.isReconciled());
        return true;
    }
}
