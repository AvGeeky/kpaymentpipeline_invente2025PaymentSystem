package com.saipbuilds.kpaymentpipeline.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconciledEvent {
    private String emailID;
    private String paymentID;
    private String name;
    private String phoneNumber;
    private String method;
    private String paidOn;
    private boolean reconciled;
    private String amount;
    private String createdAt;
    private Map<Integer,String> eventBookingDetails;
}
