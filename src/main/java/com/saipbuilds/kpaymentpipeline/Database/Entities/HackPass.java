package com.saipbuilds.kpaymentpipeline.Database.Entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HackPass {
    private String teamId;        // PK
    private String leaderEmail;
    private String teamName;
    private String track;
    private String paymentId;
    private boolean ticketIssued;
    private Instant createdAt;
}
