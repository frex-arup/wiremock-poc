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
    
    @Bean
    public WireMockServer wireMockServer() {
        WireMockServer server = new WireMockServer(WireMockConfiguration.options()
                .port(wireMockPort));
        server.start();
        System.out.println("Wiremock Server started at " + wireMockPort);
        return server;
    }
}