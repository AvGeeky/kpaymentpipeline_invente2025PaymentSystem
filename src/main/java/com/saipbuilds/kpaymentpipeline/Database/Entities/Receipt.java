package com.saipbuilds.kpaymentpipeline.Database.Entities;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class Receipt {
    private String paymentId;
    private String paidOn;
    private String method;
    private String email;
    private String phone;
    private boolean passGenerated;
    private String amount;
}

