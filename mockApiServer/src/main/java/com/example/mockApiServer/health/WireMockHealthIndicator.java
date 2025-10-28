package com.example.mockApiServer.health;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for WireMock server
 */
@Component
public class WireMockHealthIndicator implements HealthIndicator {
    
    private final WireMockServer wireMockServer;
    
    public WireMockHealthIndicator(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }
    
    @Override
    public Health health() {
        try {
            if (wireMockServer.isRunning()) {
                return Health.up()
                        .withDetail("port", wireMockServer.port())
                        .withDetail("mappings", wireMockServer.getStubMappings().size())
                        .withDetail("status", "running")
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "stopped")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
