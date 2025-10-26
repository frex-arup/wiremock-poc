package com.example.source;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/source")
public class SourceController {

    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${wiremock.url:http://localhost:9090}")
    private String wireMockUrl;

    @GetMapping("/clients/{id}")
    public String getClient(@PathVariable String id) {
        return restTemplate.getForObject(wireMockUrl + "/api/clients/" + id, String.class);
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable String id) {
        return restTemplate.getForObject(wireMockUrl + "/api/orders/" + id, String.class);
    }

    @PostMapping("/clients")
    public String createClient(@RequestBody String client) {
        return restTemplate.postForObject(wireMockUrl + "/api/clients", client, String.class);
    }
}