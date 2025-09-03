
package com.saipbuilds.kpaymentpipeline.Auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
@Slf4j
public class HmacAuthInterceptor implements HandlerInterceptor {

    private final String sharedSecret = System.getenv("HMAC_SHARED_SECRET");
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long MAX_REQUEST_AGE_SECONDS = 300; // 5 minutes

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String receivedSignature = request.getHeader("X-Signature");
        String requestTimestampStr = request.getHeader("X-Timestamp");

        if (receivedSignature == null || requestTimestampStr == null) {
            log.warn("Missing signature or timestamp headers.");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing authentication headers");
            return false;
        }

        // 1. Check the timestamp to prevent replay attacks
        long requestTimestamp = Long.parseLong(requestTimestampStr);
        long currentTimestamp = Instant.now().getEpochSecond();

        if (Math.abs(currentTimestamp - requestTimestamp) > MAX_REQUEST_AGE_SECONDS) {
            log.warn("Stale request. Possible replay attack. Timestamp: {}", requestTimestamp);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Request timestamp is too old");
            return false;
        }

        // 2. The data to sign is NOW ONLY the timestamp string
        String dataToSign = requestTimestampStr;
        String computedSignature = calculateHmac(dataToSign);

        // 3. Compare signatures using a timing-safe method
        if (!MessageDigest.isEqual(computedSignature.getBytes(), receivedSignature.getBytes())) {
            log.warn("Invalid signature. Computed: [{}], Received: [{}]", computedSignature, receivedSignature);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid signature");
            return false;
        }

        log.info("HMAC (timestamp only) signature verified successfully.");
        return true;
    }

    private String calculateHmac(String data) throws Exception {
        if (sharedSecret == null) {
            log.error("HMAC_SHARED_SECRET environment variable not set!");
            throw new IllegalStateException("HMAC secret key is not configured.");
        }
        Mac sha256Hmac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}