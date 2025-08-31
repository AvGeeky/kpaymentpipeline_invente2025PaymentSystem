package com.saipbuilds.kpaymentpipeline.Database.Entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Pass {
    private String uuid;// PK
    private String userEmail;   // FK → users.email
    private String paymentId;   // FK → receipts.payment_id
    private boolean ticketIssued;
    private String createdAt;
}
