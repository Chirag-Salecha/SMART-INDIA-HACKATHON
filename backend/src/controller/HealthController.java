package com.sih.bidcompliance.controller;

import com.sih.bidcompliance.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Basic health check so the frontend / infra can confirm the backend is up.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "bid-compliance-backend");
    }
}
