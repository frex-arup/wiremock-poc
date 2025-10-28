package com.example.mockApiServer.service.storage;

import com.example.mockApiServer.config.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    
    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private final StorageConfig.LocalConfig config;
    private final Path storageDirectory;
    
    public LocalStorageService(StorageConfig storageConfig) throws IOException {
        this.config = storageConfig.getLocal();
        this.storageDirectory = Paths.get(config.getDirectory());
        Files.createDirectories(storageDirectory);
        log.info("Local storage initialized at: {}", storageDirectory.toAbsolutePath());
    }
    
    @Override
    public void saveSnapshot(String name, byte[] data) throws IOException {
        Path snapshotPath = storageDirectory.resolve(name + ".zip");
        Files.write(snapshotPath, data);
        log.info("Snapshot saved locally: {}", snapshotPath.toAbsolutePath());
    }
    
    @Override
    public byte[] loadSnapshot(String name) throws IOException {
        Path snapshotPath = storageDirectory.resolve(name + ".zip");
        if (!Files.exists(snapshotPath)) {
            throw new IOException("Snapshot not found: " + name);
        }
        log.info("Loading snapshot from: {}", snapshotPath.toAbsolutePath());
        return Files.readAllBytes(snapshotPath);
    }
    
    @Override
    public List<String> listSnapshots() throws IOException {
        if (!Files.exists(storageDirectory)) {
            return List.of();
        }
        
        try (Stream<Path> paths = Files.list(storageDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".zip"))
                    .map(path -> path.getFileName().toString().replace(".zip", ""))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
    
    @Override
    public boolean deleteSnapshot(String name) throws IOException {
        Path snapshotPath = storageDirectory.resolve(name + ".zip");
        if (Files.exists(snapshotPath)) {
            Files.delete(snapshotPath);
            log.info("Deleted snapshot: {}", name);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean snapshotExists(String name) throws IOException {
        Path snapshotPath = storageDirectory.resolve(name + ".zip");
        return Files.exists(snapshotPath);
    }
}
