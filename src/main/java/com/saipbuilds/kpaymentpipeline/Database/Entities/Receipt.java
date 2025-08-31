package com.saipbuilds.kpaymentpipeline.Database.Entities;


import lombok.Data;
import java.time.Instant;

@Data
public class Receipt {
    private String paymentId;
    private Instant paidOn;
    private String method;
    private String email;
    private String phone;
}

