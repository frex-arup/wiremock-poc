package com.example.mockApiServer.service.storage;

import com.example.mockApiServer.config.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "NEXUS")
public class NexusStorageService implements StorageService {
    
    private static final Logger log = LoggerFactory.getLogger(NexusStorageService.class);
    private final StorageConfig.NexusConfig config;
    private final RestTemplate restTemplate;
    
    public NexusStorageService(StorageConfig storageConfig) {
        this.config = storageConfig.getNexus();
        
        if (config.getUrl() == null || config.getUrl().isEmpty()) {
            throw new IllegalStateException("Nexus URL is required for NEXUS storage type");
        }
        
        if (config.getRepository() == null || config.getRepository().isEmpty()) {
            throw new IllegalStateException("Nexus repository is required for NEXUS storage type");
        }
        
        this.restTemplate = new RestTemplate();
        log.info("Nexus storage initialized for repository: {}", config.getRepository());
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        
        if (config.getUsername() != null && config.getPassword() != null) {
            String auth = config.getUsername() + ":" + config.getPassword();
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);
            headers.set("Authorization", authHeader);
        }
        
        return headers;
    }
    
    private String getArtifactUrl(String name) {
        // Format: {nexusUrl}/repository/{repositoryName}/{groupId}/{artifactId}/{version}/{artifactId}-{version}.zip
        String groupPath = config.getGroupId().replace(".", "/");
        return String.format("%s/repository/%s/%s/%s/%s/%s-%s.zip",
                config.getUrl().replaceAll("/+$", ""),
                config.getRepository(),
                groupPath,
                config.getArtifactId(),
                name,
                config.getArtifactId(),
                name);
    }
    
    @Override
    public void saveSnapshot(String name, byte[] data) throws IOException {
        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        
        HttpEntity<byte[]> entity = new HttpEntity<>(data, headers);
        String url = getArtifactUrl(name);
        
        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Snapshot saved to Nexus: {}", name);
        } catch (Exception e) {
            throw new IOException("Failed to save snapshot to Nexus: " + name, e);
        }
    }
    
    @Override
    public byte[] loadSnapshot(String name) throws IOException {
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = getArtifactUrl(name);
        
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class);
            
            log.info("Snapshot loaded from Nexus: {}", name);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new IOException("Snapshot not found in Nexus: " + name, e);
        } catch (Exception e) {
            throw new IOException("Failed to load snapshot from Nexus: " + name, e);
        }
    }
    
    @Override
    public List<String> listSnapshots() throws IOException {
        // Note: Nexus REST API v3 is required for listing artifacts
        // This is a simplified implementation
        // For production, you would need to use Nexus REST API to query for artifacts
        log.warn("Listing snapshots from Nexus is not fully implemented. Using Nexus REST API v3 is recommended.");
        return Collections.emptyList();
    }
    
    @Override
    public boolean deleteSnapshot(String name) throws IOException {
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = getArtifactUrl(name);
        
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("Snapshot deleted from Nexus: {}", name);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Snapshot not found in Nexus for deletion: {}", name);
            return false;
        } catch (Exception e) {
            throw new IOException("Failed to delete snapshot from Nexus: " + name, e);
        }
    }
    
    @Override
    public boolean snapshotExists(String name) throws IOException {
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String url = getArtifactUrl(name);
        
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    entity,
                    Void.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking if snapshot exists in Nexus: {}", name, e);
            return false;
        }
    }
}
