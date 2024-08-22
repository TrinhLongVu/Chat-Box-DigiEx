package com.example.client.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class ResponseLoadBalancer {
    private String response;
    private boolean isRunning = false;
}
