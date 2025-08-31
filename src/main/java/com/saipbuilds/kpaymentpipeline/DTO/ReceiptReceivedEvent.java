package com.saipbuilds.kpaymentpipeline.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptReceivedEvent {
    private String emailID;
    private String paymentID;
    private String paidOn;
    private String method;
    private String amount;
    private String phoneNumber;
}
