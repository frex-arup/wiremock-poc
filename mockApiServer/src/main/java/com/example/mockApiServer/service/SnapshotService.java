package com.example.mockApiServer.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class SnapshotService {
    
    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);
    
    @Value("${snapshot.source:filesystem}")
    private String snapshotSource;
    
    @Value("${snapshot.version:latest}")
    private String snapshotVersion;
    
    @Value("${snapshot.git.url:}")
    private String gitUrl;
    
    @Value("${snapshot.nexus.url:}")
    private String nexusUrl;
    
    @Autowired
    private WireMockServer wireMockServer;
    
    public void loadSnapshots() throws IOException {
        log.info("Loading snapshots from {} version {}", snapshotSource, snapshotVersion);
        
        switch (snapshotSource.toLowerCase()) {
            case "filesystem":
                loadFromFilesystem();
                break;
            case "git":
                loadFromGit();
                break;
            case "nexus":
                loadFromNexus();
                break;
            default:
                throw new IllegalArgumentException("Unsupported snapshot source: " + snapshotSource);
        }
    }
    
    private void loadFromFilesystem() throws IOException {
        String snapshotDir = "./test";
        copySnapshotsToWiremock(snapshotDir);
    }
    
    private void loadFromGit() {
        log.info("Loading from Git: {}", gitUrl);
        try {
            loadFromFilesystem();
        } catch (IOException e) {
            log.error("Failed to load from Git", e);
        }
    }
    
    private void loadFromNexus() {
        log.info("Loading from Nexus: {}", nexusUrl);
        try {
            loadFromFilesystem();
        } catch (IOException e) {
            log.error("Failed to load from Nexus", e);
        }
    }
    
    private void copySnapshotsToWiremock(String sourceDir) throws IOException {
        Path sourcePath = Paths.get(sourceDir);
        if (!Files.exists(sourcePath)) {
            log.warn("Snapshot directory not found: {}", sourceDir);
            return;
        }
        
        // Copy mappings
        Path mappingsSource = sourcePath.resolve("mappings");
        Path mappingsTarget = Paths.get("./wiremock/mappings");
        if (Files.exists(mappingsSource)) {
            copyDirectory(mappingsSource, mappingsTarget);
        }
        
        // Copy response files
        Path filesSource = sourcePath.resolve("__files");
        Path filesTarget = Paths.get("./wiremock/__files");
        if (Files.exists(filesSource)) {
            copyDirectory(filesSource, filesTarget);
        }
        
        log.info("Snapshots loaded from {}", sourceDir);
    }
    
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        
        try (Stream<Path> paths = Files.walk(source)) {
            paths.filter(Files::isRegularFile)
                 .forEach(sourcePath -> {
                     try {
                         Path targetPath = target.resolve(source.relativize(sourcePath));
                         Files.createDirectories(targetPath.getParent());
                         Files.copy(sourcePath, targetPath, 
                                  java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                     } catch (IOException e) {
                         log.error("Failed to copy file: {}", sourcePath, e);
                     }
                 });
        }
    }
    
    public List<String> getAvailableVersions() throws IOException {
        Path snapshotsDir = Paths.get("./snapshots");
        if (!Files.exists(snapshotsDir)) {
            return List.of();
        }
        
        try (Stream<Path> paths = Files.list(snapshotsDir)) {
            return paths.filter(Files::isDirectory)
                       .map(path -> path.getFileName().toString())
                       .sorted()
                       .collect(java.util.stream.Collectors.toList());
        }
    }
}