package com.saipbuilds.kpaymentpipeline.Database.Entities;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class Pass {
    private String uuid;// PK
    private String userEmail;   // FK → users.email
    private String paymentId;   // FK → receipts.payment_id
    private boolean ticketIssued;
    private Instant createdAt;
}
