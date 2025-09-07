package com.saipbuilds.kpaymentpipeline.Controller;

import com.saipbuilds.kpaymentpipeline.DTO.HackathonPaymentReceivedEvent;
import com.saipbuilds.kpaymentpipeline.Kafka.Producers.EventProducer;
import com.saipbuilds.kpaymentpipeline.TicketSender.emailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sendOTP")
@RequiredArgsConstructor
@Slf4j
public class OTPController {
    @Autowired
    emailUtil emailutil;


    @PostMapping
    public ResponseEntity<?> sendOTP(@RequestBody Map<String, String> payload) {
        try {
            String email = payload.get("email");
            String otp = payload.get("otp");

            if (email == null || otp == null) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse("Missing required fields", 400)
                );
            }

            // Call your email service to send the OTP

            emailutil.sendOTPEmail(email, otp);

            log.info("OTP sent to email: {}", email);

            return ResponseEntity.status(201).body(
                    new ApiResponse("OTP sent to email", 201)
            );
        } catch (Exception e) {
            log.error("Error sending OTP", e);
            return ResponseEntity.status(500).body(
                    new ApiResponse("Failed to send OTP", 500)
            );
        }
    }
}
