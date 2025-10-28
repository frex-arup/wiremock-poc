package com.example.mockApiServer.service.storage;

import com.example.mockApiServer.config.StorageConfig;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "GITHUB")
public class GitHubStorageService implements StorageService {
    
    private static final Logger log = LoggerFactory.getLogger(GitHubStorageService.class);
    private final StorageConfig.GitHubConfig config;
    private final GitHub gitHub;
    
    public GitHubStorageService(StorageConfig storageConfig) throws IOException {
        this.config = storageConfig.getGithub();
        
        if (config.getToken() == null || config.getToken().isEmpty()) {
            throw new IllegalStateException("GitHub token is required for GITHUB storage type");
        }
        
        if (config.getRepository() == null || config.getRepository().isEmpty()) {
            throw new IllegalStateException("GitHub repository is required for GITHUB storage type");
        }
        
        this.gitHub = new GitHubBuilder().withOAuthToken(config.getToken()).build();
        log.info("GitHub storage initialized for repository: {}", config.getRepository());
    }
    
    @Override
    public void saveSnapshot(String name, byte[] data) throws IOException {
        GHRepository repository = gitHub.getRepository(config.getRepository());
        String path = config.getBaseDir() + "/" + name + ".zip";
        
        try {
            // Try to get existing file to update it
            GHContent existingFile = repository.getFileContent(path, config.getBranch());
            existingFile.update(data, "Update WireMock snapshot: " + name, config.getBranch());
            log.info("Updated GitHub snapshot: {}", name);
        } catch (GHFileNotFoundException e) {
            // File doesn't exist, create new one
            repository.createContent()
                    .content(data)
                    .message("Add WireMock snapshot: " + name)
                    .branch(config.getBranch())
                    .path(path)
                    .commit();
            log.info("Created GitHub snapshot: {}", name);
        }
    }
    
    @Override
    public byte[] loadSnapshot(String name) throws IOException {
        GHRepository repository = gitHub.getRepository(config.getRepository());
        String path = config.getBaseDir() + "/" + name + ".zip";
        
        try {
            GHContent content = repository.getFileContent(path, config.getBranch());
            
            // GitHub API returns base64 encoded content
            if (content.isFile()) {
                String encodedContent = content.getContent();
                byte[] decodedContent = Base64.getMimeDecoder().decode(encodedContent.replaceAll("\\s", ""));
                log.info("Loaded GitHub snapshot: {}", name);
                return decodedContent;
            } else {
                throw new IOException("Path is not a file: " + path);
            }
        } catch (GHFileNotFoundException e) {
            throw new IOException("Snapshot not found in GitHub: " + name, e);
        }
    }
    
    @Override
    public List<String> listSnapshots() throws IOException {
        try {
            GHRepository repository = gitHub.getRepository(config.getRepository());
            List<GHContent> contents = repository.getDirectoryContent(config.getBaseDir(), config.getBranch());
            
            return contents.stream()
                    .filter(GHContent::isFile)
                    .map(GHContent::getName)
                    .filter(name -> name.endsWith(".zip"))
                    .map(name -> name.replace(".zip", ""))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (GHFileNotFoundException e) {
            log.warn("Snapshot directory not found in GitHub: {}", config.getBaseDir());
            return List.of();
        }
    }
    
    @Override
    public boolean deleteSnapshot(String name) throws IOException {
        try {
            GHRepository repository = gitHub.getRepository(config.getRepository());
            String path = config.getBaseDir() + "/" + name + ".zip";
            GHContent content = repository.getFileContent(path, config.getBranch());
            content.delete("Delete WireMock snapshot: " + name, config.getBranch());
            log.info("Deleted GitHub snapshot: {}", name);
            return true;
        } catch (GHFileNotFoundException e) {
            log.warn("Snapshot not found for deletion: {}", name);
            return false;
        }
    }
    
    @Override
    public boolean snapshotExists(String name) throws IOException {
        try {
            GHRepository repository = gitHub.getRepository(config.getRepository());
            String path = config.getBaseDir() + "/" + name + ".zip";
            repository.getFileContent(path, config.getBranch());
            return true;
        } catch (GHFileNotFoundException e) {
            return false;
        }
    }
}
