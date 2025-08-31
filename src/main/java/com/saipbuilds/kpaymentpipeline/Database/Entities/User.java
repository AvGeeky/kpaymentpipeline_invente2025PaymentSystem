package com.saipbuilds.kpaymentpipeline.Database.Entities;

import lombok.Data;
import java.time.Instant;

@Data
public class User {
    private String email;
    private String name;
    private String phone;
    private Instant createdAt;
    private boolean statusPaid;
}
