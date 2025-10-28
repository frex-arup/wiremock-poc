package com.example.mockApiServer.health;

import com.example.mockApiServer.config.StorageConfig;
import com.example.mockApiServer.service.storage.StorageService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for storage service
 */
@Component
public class StorageHealthIndicator implements HealthIndicator {
    
    private final StorageService storageService;
    private final StorageConfig storageConfig;
    
    public StorageHealthIndicator(StorageService storageService, StorageConfig storageConfig) {
        this.storageService = storageService;
        this.storageConfig = storageConfig;
    }
    
    @Override
    public Health health() {
        try {
            // Try to list snapshots to verify connection
            storageService.listSnapshots();
            
            return Health.up()
                    .withDetail("type", storageConfig.getType())
                    .withDetail("status", "connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("type", storageConfig.getType())
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
