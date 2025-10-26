package com.example.source;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Configuration
public class WireMockProxyConfig {

    @Bean
    public WireMockServer wireMockServer() {
        WireMockServer server = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .port(9090)
                .enableBrowserProxying(true)
                .usingFilesUnderDirectory("src/test/resources"));
        
        server.start();
        
        // Enable recording mode - records all interactions
        server.startRecording("http://localhost:8081");
        
        return server;
    }
}