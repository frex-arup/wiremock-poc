package com.example.mockApiServer.controller;

import com.example.mockApiServer.service.storage.StorageService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/snapshots")
public class SnapshotController {
    
    private static final Logger log = LoggerFactory.getLogger(SnapshotController.class);
    
    @Autowired
    private StorageService storageService;
    
    @Autowired
    private WireMockServer wireMockServer;
    
    /**
     * Create a new snapshot from current WireMock state
     */
    @PostMapping("/{name}")
    public ResponseEntity<Map<String, String>> createSnapshot(@PathVariable String name) {
        try {
            log.info("Creating snapshot: {}", name);
            
            // Create a zip file containing mappings and files
            byte[] snapshotData = createSnapshotZip();
            
            // Save to storage backend
            storageService.saveSnapshot(name, snapshotData);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Snapshot created successfully");
            response.put("name", name);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create snapshot: {}", name, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create snapshot: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Create a snapshot with auto-generated timestamp name
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createAutoSnapshot() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String name = "snapshot-" + timestamp;
        return createSnapshot(name);
    }
    
    /**
     * Restore a snapshot to WireMock
     */
    @PostMapping("/{name}/restore")
    public ResponseEntity<Map<String, String>> restoreSnapshot(@PathVariable String name) {
        try {
            log.info("Restoring snapshot: {}", name);
            
            // Load snapshot data from storage
            byte[] snapshotData = storageService.loadSnapshot(name);
            
            // Extract and restore to WireMock
            restoreSnapshotFromZip(snapshotData);
            
            // Reload WireMock server to pick up new mappings
            wireMockServer.resetAll();
            reloadWireMockMappings();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Snapshot restored successfully");
            response.put("name", name);
            response.put("mappings", String.valueOf(wireMockServer.getStubMappings().size()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to restore snapshot: {}", name, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to restore snapshot: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * List all available snapshots
     */
    @GetMapping
    public ResponseEntity<List<String>> listSnapshots() {
        try {
            List<String> snapshots = storageService.listSnapshots();
            return ResponseEntity.ok(snapshots);
        } catch (Exception e) {
            log.error("Failed to list snapshots", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
    
    /**
     * Delete a snapshot
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<Map<String, String>> deleteSnapshot(@PathVariable String name) {
        try {
            boolean deleted = storageService.deleteSnapshot(name);
            
            Map<String, String> response = new HashMap<>();
            if (deleted) {
                response.put("message", "Snapshot deleted successfully");
                response.put("name", name);
                return ResponseEntity.ok(response);
            } else {
                response.put("error", "Snapshot not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Failed to delete snapshot: {}", name, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete snapshot: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Check if a snapshot exists
     */
    @GetMapping("/{name}/exists")
    public ResponseEntity<Map<String, Boolean>> snapshotExists(@PathVariable String name) {
        try {
            boolean exists = storageService.snapshotExists(name);
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to check snapshot existence: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("exists", false));
        }
    }
    
    /**
     * Create a zip file from current WireMock mappings and files
     */
    private byte[] createSnapshotZip() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add mappings
            Path mappingsDir = Paths.get("./wiremock/mappings");
            if (Files.exists(mappingsDir)) {
                addDirectoryToZip(zos, mappingsDir, "mappings");
            }
            
            // Add files
            Path filesDir = Paths.get("./wiremock/__files");
            if (Files.exists(filesDir)) {
                addDirectoryToZip(zos, filesDir, "__files");
            }
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Add a directory to zip recursively
     */
    private void addDirectoryToZip(ZipOutputStream zos, Path sourceDir, String zipPath) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     try {
                         String relativePath = sourceDir.relativize(path).toString();
                         String entryName = zipPath + "/" + relativePath.replace("\\", "/");
                         
                         ZipEntry zipEntry = new ZipEntry(entryName);
                         zos.putNextEntry(zipEntry);
                         Files.copy(path, zos);
                         zos.closeEntry();
                     } catch (IOException e) {
                         log.error("Failed to add file to zip: {}", path, e);
                     }
                 });
        }
    }
    
    /**
     * Restore snapshot from zip data
     */
    private void restoreSnapshotFromZip(byte[] zipData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bais)) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                
                Path outputPath = Paths.get("./wiremock/" + entry.getName());
                Files.createDirectories(outputPath.getParent());
                
                try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                
                zis.closeEntry();
            }
        }
    }
    
    /**
     * Reload WireMock mappings from disk
     */
    private void reloadWireMockMappings() throws IOException {
        Path mappingsDir = Paths.get("./wiremock/mappings");
        if (!Files.exists(mappingsDir)) {
            return;
        }
        
        try (Stream<Path> paths = Files.list(mappingsDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".json"))
                 .forEach(path -> {
                     try {
                         String mappingJson = Files.readString(path);
                         StubMapping mapping = StubMapping.buildFrom(mappingJson);
                         wireMockServer.addStubMapping(mapping);
                         log.debug("Loaded mapping from: {}", path.getFileName());
                     } catch (Exception e) {
                         log.error("Failed to load mapping from: {}", path, e);
                     }
                 });
        }
    }
}
