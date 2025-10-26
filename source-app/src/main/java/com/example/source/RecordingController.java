package com.example.source;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recording")
public class RecordingController {

    @Autowired
    private WireMockServer wireMockServer;

    @PostMapping("/start")
    public String startRecording() {
        wireMockServer.startRecording("http://localhost:8081");
        return "Recording started";
    }

    @PostMapping("/stop")
    public String stopRecording() {
        wireMockServer.stopRecording();
        return "Recording stopped";
    }

    @GetMapping("/mappings")
    public String getMappings() {
        return wireMockServer.getAllServeEvents().toString();
    }
}