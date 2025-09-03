package com.saipbuilds.kpaymentpipeline.Controller;

import com.saipbuilds.kpaymentpipeline.Email.EmailReceiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system")
public class HealthController {

    private final RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private final EmailReceiverService emailReceiverService; // <-- inject your dependency

    private final StringRedisSerializer stringSerializer = new StringRedisSerializer();

    private static final String HEARTBEAT_KEY = "email-listener:heartbeat";
    private static final Duration DEFAULT_MAX_HEARTBEAT_AGE = Duration.ofSeconds(90);

    // ---------------- Health endpoint ----------------
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health(
            @RequestParam(value = "maxAgeSeconds", required = false) Long maxAgeSeconds
    ) {
        Duration maxAge = (maxAgeSeconds == null)
                ? DEFAULT_MAX_HEARTBEAT_AGE
                : Duration.ofSeconds(maxAgeSeconds);

        Map<String, Object> resp = new HashMap<>();
        resp.put("key", HEARTBEAT_KEY);

        // Add detailed health info from dependent services
        Map<String, Object> dependencyHealth = emailReceiverService.getHealthStatus();
        resp.put("dependencies", dependencyHealth);

        try {
            Object rawObj = redisTemplate.opsForValue().get(HEARTBEAT_KEY);
            String raw = rawObj == null ? null : rawObj.toString();
            resp.put("lastHeartbeat", raw);

            if (raw == null) {
                resp.put("status", "DOWN");
                resp.put("reason", "No heartbeat key found in Redis");
                return ResponseEntity.status(200).body(resp);
            }

            Instant last;
            try {
                last = Instant.parse(raw);
            } catch (DateTimeParseException e) {
                resp.put("status", "DOWN");
                resp.put("reason", "Invalid timestamp stored in heartbeat");
                return ResponseEntity.status(200).body(resp);
            }

            Duration age = Duration.between(last, Instant.now());
            resp.put("ageSeconds", age.getSeconds());

            if (age.compareTo(maxAge) <= 0) {
                resp.put("status", "UP");
                return ResponseEntity.ok(resp);
            } else {
                resp.put("status", "STALE");
                resp.put("reason", "Heartbeat older than allowed maxAgeSeconds=" + maxAge.getSeconds());
                return ResponseEntity.status(200).body(resp);
            }

        } catch (Exception e) {
            log.error("Failed to read heartbeat from Redis", e);
            resp.put("status", "DOWN");
            resp.put("reason", "Redis read error: " + e.getMessage());
            return ResponseEntity.status(200).body(resp);
        }
    }
}
