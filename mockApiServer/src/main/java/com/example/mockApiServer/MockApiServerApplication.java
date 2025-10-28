package com.example.mockApiServer;

import com.example.mockApiServer.config.StorageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageConfig.class)
public class MockApiServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MockApiServerApplication.class, args);
	}

}
