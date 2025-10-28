package com.example.mockApiServer.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WireMockConfig {
    
    @Value("${wiremock.server.port:8089}")
    private int wireMockPort;
    
    @Value("${wiremock.mode:STUB}")
    private String mode;
    
    @Value("${wiremock.proxy-url:http://localhost:8081}")
    private String proxyUrl;
    
    @Bean
    public WireMockServer wireMockServer() {
        WireMockServer server = new WireMockServer(WireMockConfiguration.options()
                .port(wireMockPort)
                .usingFilesUnderDirectory("./wiremock"));
        
        server.start();
        
        // Configure based on mode
        if ("PROXY".equalsIgnoreCase(mode)) {
            // Proxy mode: forward all requests to target and record
            server.startRecording(proxyUrl);
            System.out.println("✅ WireMock started in PROXY mode, recording to: " + proxyUrl);
        } else {
            // Stub mode: serve recorded responses
            System.out.println("✅ WireMock started in STUB mode, serving mocked responses");
        }
        
        System.out.println("✅ WireMock Server started at port " + wireMockPort);
        return server;
    }
}
