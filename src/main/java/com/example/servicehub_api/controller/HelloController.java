package com.example.servicehub_api.controller;

import com.example.servicehub_api.model.StatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1", "/api"})
public class HelloController {
    private final String version;

    public HelloController(@Value("${app.version}") String version) {
        this.version = version;
    }

    @GetMapping({"/info", "/hello"})
    public StatusResponse info() {
        return new StatusResponse("OK", version, "ServiceHub API operacional");
    }
}
