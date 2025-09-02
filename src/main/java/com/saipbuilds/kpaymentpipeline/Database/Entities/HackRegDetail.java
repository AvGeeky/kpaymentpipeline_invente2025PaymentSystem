package com.saipbuilds.kpaymentpipeline.Database.Entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HackRegDetail {
    private String teamId;       // FK → hack_passes.team_id
    private String email;        // participant email (part of PK)
    private String fullName;
    private String institution;
    private Instant createdAt;
    private String phoneNumber;
}
