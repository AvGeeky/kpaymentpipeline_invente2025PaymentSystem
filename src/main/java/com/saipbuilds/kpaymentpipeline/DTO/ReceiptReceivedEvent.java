package com.saipbuilds.kpaymentpipeline.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReceiptReceivedEvent {
    private String emailID;
    private String paymentID;
    private String paidOn;
    private String method;
    private String amount;
    private String phoneNumber;

}
