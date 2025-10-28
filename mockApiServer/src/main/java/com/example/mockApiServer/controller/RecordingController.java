package com.example.mockApiServer.controller;

import com.example.mockApiServer.service.storage.StorageService;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Recording Controller - Manages WireMock recording mode
 */
@RestController
@RequestMapping("/wiremock/recording")
public class RecordingController {

    private static final Logger log = LoggerFactory.getLogger(RecordingController.class);

    @Autowired
    private WireMockServer wireMockServer;
    
    @Value("${wiremock.proxy-url:http://localhost:8081}")
    private String proxyUrl;
    
    private boolean isRecording = false;

    /**
     * Start recording HTTP traffic
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startRecording() {
        Map<String, String> response = new HashMap<>();
        
        if (!isRecording) {
            try {
                wireMockServer.startRecording(proxyUrl);
                isRecording = true;
                log.info("Recording started, proxying to: {}", proxyUrl);
                
                response.put("status", "recording");
                response.put("proxyUrl", proxyUrl);
                response.put("message", "Recording started successfully");
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Failed to start recording", e);
                response.put("status", "error");
                response.put("message", "Failed to start recording: " + e.getMessage());
                return ResponseEntity.status(500).body(response);
            }
        } else {
            response.put("status", "already_recording");
            response.put("message", "Recording is already in progress");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Stop recording and optionally create a snapshot
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopRecording(
            @RequestParam(required = false) String snapshotName) {
        Map<String, String> response = new HashMap<>();
        
        if (isRecording) {
            try {
                wireMockServer.stopRecording();
                isRecording = false;
                log.info("Recording stopped");
                
                response.put("status", "stopped");
                response.put("message", "Recording stopped successfully");
                response.put("mappingsRecorded", String.valueOf(wireMockServer.getStubMappings().size()));
                
                // Optionally create snapshot
                if (snapshotName != null && !snapshotName.isEmpty()) {
                    response.put("snapshot", snapshotName);
                    response.put("message", "Recording stopped. Create snapshot using: POST /api/snapshots/" + snapshotName);
                }
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Failed to stop recording", e);
                response.put("status", "error");
                response.put("message", "Failed to stop recording: " + e.getMessage());
                return ResponseEntity.status(500).body(response);
            }
        } else {
            response.put("status", "not_recording");
            response.put("message", "Recording is not active");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get current recording status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getRecordingStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("isRecording", isRecording);
        response.put("proxyUrl", proxyUrl);
        response.put("mappingsCount", wireMockServer.getStubMappings().size());
        response.put("wireMockPort", wireMockServer.port());
        response.put("isRunning", wireMockServer.isRunning());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reset all mappings (clear recorded stubs)
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetMappings() {
        Map<String, String> response = new HashMap<>();
        
        try {
            wireMockServer.resetAll();
            log.info("All WireMock mappings reset");
            
            response.put("status", "success");
            response.put("message", "All mappings have been reset");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to reset mappings", e);
            response.put("status", "error");
            response.put("message", "Failed to reset mappings: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
