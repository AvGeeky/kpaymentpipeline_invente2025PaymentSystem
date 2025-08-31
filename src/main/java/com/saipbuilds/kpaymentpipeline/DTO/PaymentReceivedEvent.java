package com.saipbuilds.kpaymentpipeline.DTO;

// Payment initiated by frontend

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReceivedEvent {
    private String emailID;
    private String paymentID;
    private String name;
    private String phoneNumber;
    private String createdAt;
    private List<Map<Integer,Integer>> eventBookingDetails;
}



