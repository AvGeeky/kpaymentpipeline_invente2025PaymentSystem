package com.saipbuilds.kpaymentpipeline.Kafka.Reconcillation;

import com.saipbuilds.kpaymentpipeline.DTO.ReconciledEvent;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.PassMapper;
import com.saipbuilds.kpaymentpipeline.Database.Mapper.ReceiptMapper;
import com.saipbuilds.kpaymentpipeline.Service.PaymentDatabaseServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class DuringReconciliationService {
    private final ReceiptMapper receiptMapper;
    PaymentDatabaseServices paymentDatabaseServices;

    String amountString = System.getenv("AMOUNT_PERPASS");
    Integer amountPerPass = Integer.parseInt(amountString != null ? amountString : "300");
    private final PassMapper passMapper;

    public DuringReconciliationService(PassMapper passMapper, PaymentDatabaseServices paymentDatabaseServices, ReceiptMapper receiptMapper) {
        this.passMapper = passMapper;
        this.paymentDatabaseServices=paymentDatabaseServices;
        this.receiptMapper = receiptMapper;
    }

    public boolean handleReconciliation(ReconciledEvent event) {

        int expectedAmount = amountPerPass * event.getEventBookingDetails().size();
        boolean b=receiptMapper.isPassGenerated(event.getPaymentID());
        if (b){
            log.warn(" Reconciliation skipped: Passes already generated for paymentID={} email={}",
                    event.getPaymentID(), event.getEmailID());
            return false;
        }

        if ((int) Double.parseDouble(event.getAmount()) != expectedAmount) {
            if (!event.getType().equalsIgnoreCase("n")) {
                log.warn(" Reconciliation skipped: Mismatch in number of passes={} for paymentID={} email={}",event.getEventBookingDetails().size(),
                        event.getPaymentID(), event.getEmailID());
                return false;
            }
        }
        for(Map<Integer,Integer> pass : event.getEventBookingDetails()){
            String uuid = paymentDatabaseServices.insertOnePassAndReturnUUID(event.getEmailID(), event.getPaymentID(), event.getType());
            paymentDatabaseServices.insertIntoSlots(uuid,pass);
        }
        receiptMapper.markPassGeneratedTrue(event.getPaymentID());
        event.setReconciled(true);


        log.info(" S3 Reconciled email={} paymentId={} reconciled={}",
                event.getEmailID(), event.getPaymentID(), event.isReconciled());
        return true;
    }
}
