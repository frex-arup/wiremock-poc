package com.example.mockApiServer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageConfig {
    private StorageType type = StorageType.LOCAL;
    private GitHubConfig github = new GitHubConfig();
    private NexusConfig nexus = new NexusConfig();
    private LocalConfig local = new LocalConfig();

    public enum StorageType {
        GITHUB, NEXUS, LOCAL
    }

    @Getter
    @Setter
    public static class GitHubConfig {
        private String repository;
        private String branch = "main";
        private String token;
        private String baseDir = "snapshots";
        private String username;
    }

    @Getter
    @Setter
    public static class NexusConfig {
        private String url;
        private String repository;
        private String username;
        private String password;
        private String groupId = "com.example";
        private String artifactId = "wiremock-snapshots";
    }

    @Getter
    @Setter
    public static class LocalConfig {
        private String directory = "./snapshots";
    }
}
