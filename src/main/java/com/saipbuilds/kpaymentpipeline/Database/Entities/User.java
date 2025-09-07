package com.saipbuilds.kpaymentpipeline.Database.Entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;

@Data
@AllArgsConstructor
public class User {
    private String email;
    private String name;
    private String phone;
    private String createdAt;
    private String institution;
}
