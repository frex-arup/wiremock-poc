package com.example.target;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TargetController {

    @GetMapping("/clients/{id}")
    public String getClient(@PathVariable String id) {
        return "{\"id\":" + id + ",\"name\":\"Client " + id + "\",\"email\":\"client" + id + "@example.com\"}";
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable String id) {
        return "{\"id\":" + id + ",\"clientId\":1,\"product\":\"Product " + id + "\",\"amount\":100.00}";
    }

    @PostMapping("/clients")
    public String createClient(@RequestBody String client) {
        return "{\"id\":999,\"message\":\"Client created successfully\"}";
    }
}