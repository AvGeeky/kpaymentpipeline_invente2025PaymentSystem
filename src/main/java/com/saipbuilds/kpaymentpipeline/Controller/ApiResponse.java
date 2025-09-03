package com.saipbuilds.kpaymentpipeline.Controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor      // <-- ADD THIS
public class ApiResponse {
    private String message;
    private int status;
}
