package com.example.servicehub_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.servicehub_api.model.StatusResponse;



@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public StatusResponse info() {
        return new StatusResponse("OK", "1.0.0", "ServiceHub API operacional");
    }
}