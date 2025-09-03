package com.saipbuilds.kpaymentpipeline.Controller;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {
    private String message;
    private int status;
}
