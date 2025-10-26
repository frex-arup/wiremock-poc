package com.example.mockApiServer.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/wiremock")
public class WireMockController {
    
    private static final Logger log = LoggerFactory.getLogger(WireMockController.class);
    
    @Autowired
    private WireMockServer wireMockServer;
    
    @Value("${wiremock.mappings.path:./wiremock/mappings}")
    private String mappingsPath;
    
    @Value("${wiremock.files.path:./wiremock/__files}")
    private String filesPath;
    
    @Value("${wiremock.auto-load:true}")
    private boolean autoLoad;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @PostConstruct
    public void loadFromWiremockDirectory() {
        if (!autoLoad) {
            log.info("Auto-load disabled, skipping data loading");
            return;
        }
        
        try {
            File filesDir = new File(filesPath);
            
            if (!filesDir.exists()) {
                log.warn("WireMock files directory not found: {}, skipping load", filesPath);
                return;
            }
            
            File[] jsonFiles = filesDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (jsonFiles == null || jsonFiles.length == 0) {
                log.warn("No JSON files found in {}", filesPath);
                return;
            }
            
            for (File jsonFile : jsonFiles) {
                String fileName = jsonFile.getName().replace(".json", "");
                String endpoint = "/api/" + fileName;
                String responseBody = Files.readString(jsonFile.toPath());
                
                wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(endpoint))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(responseBody)));
                
                log.info("Loaded endpoint: {} from file: {}", endpoint, jsonFile.getName());
            }
            
            log.info("Dynamically loaded {} endpoints from {}", jsonFiles.length, filesPath);
        } catch (Exception e) {
            log.error("Failed to load from wiremock directory", e);
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        boolean isRunning = wireMockServer.isRunning();
        int port = wireMockServer.port();
        int mappingCount = wireMockServer.getStubMappings().size();
        
        return ResponseEntity.ok(String.format(
            "WireMock Server - Running: %s, Port: %d, Mappings: %d", 
            isRunning, port, mappingCount));
    }
    
    @GetMapping("/list/mappings")
    public ResponseEntity<List<String>> getMappings() {
        List<String> mappingUrls = wireMockServer.getStubMappings().stream()
                .map(mapping -> mapping.getRequest().getUrl())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(mappingUrls);
    }
    
    @GetMapping("/mappings/{path}")
    public ResponseEntity<String> getMappingByPath(@PathVariable String path) {
        try {
            String fullPath = "/api/" + path;
            StubMapping mapping = wireMockServer.getStubMappings().stream()
                    .filter(stub -> stub.getRequest().getUrl().equals(fullPath))
                    .findFirst()
                    .orElse(null);
            
            if (mapping == null) {
                return ResponseEntity.status(404)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Mapping not found for path: /api/" + path + "\"}");
            }
            
            String mappingJson = String.format(
                "{\"id\":\"%s\",\"request\":{\"method\":\"%s\",\"url\":\"%s\"},\"response\":{\"status\":%d,\"body\":\"%s\"}}",
                mapping.getId(),
                mapping.getRequest().getMethod(),
                mapping.getRequest().getUrl(),
                mapping.getResponse().getStatus(),
                mapping.getResponse().getBody().replace("\"", "\\\"")
            );
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(mappingJson);
        } catch (Exception e) {
            log.error("No mapping found for path: {}", path, e);
            return ResponseEntity.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"Error retrieving mapping for path: /api/" + path + "\"}");
        }
    }
    
    @GetMapping("/files/{path}")
    public ResponseEntity<String> getFileByPath(@PathVariable String path) {
        try {
            String fullPath = "/api/" + path;
            StubMapping mapping = wireMockServer.getStubMappings().stream()
                    .filter(stub -> stub.getRequest().getUrl().equals(fullPath))
                    .findFirst()
                    .orElse(null);
            
            if (mapping == null) {
                return ResponseEntity.status(404)
                        .header("Content-Type", "application/json")
                        .body("{\"error\":\"Response file not found for path: /api/" + path + "\"}");
            }
            
            String responseBody = mapping.getResponse().getBody();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(responseBody);
        } catch (Exception e) {
            log.error("No file found for path: {}", path, e);
            return ResponseEntity.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"Error retrieving file for path: /api/" + path + "\"}");
        }
    }
}