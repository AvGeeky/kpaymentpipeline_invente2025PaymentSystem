package com.saipbuilds.kpaymentpipeline.Database.Entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Slot {
    private String uuid;     // FK → pass.uuid (part of PK)
    private int slotNo;    // part of PK
    private int eventId;   // FK → events.id
    private boolean attended;
    private String createdAt;
}

