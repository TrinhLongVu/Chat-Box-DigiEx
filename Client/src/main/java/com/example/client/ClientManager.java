package com.example.client;

import com.example.client.core.ResponseLoadBalancer;
import com.example.client.utils.LoadBalanceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClientManager {
    private final Logger log = LoggerFactory.getLogger(ClientManager.class);
    private final ResponseLoadBalancer responseLoadBalancer;
    private final LoadBalanceManager loadBalanceManager;

    @Bean
    public void connectToServer() {
        boolean isConnected = false;
        String response;
        while(!isConnected) {
            try {
                Thread.sleep(2000);
                response = loadBalanceManager.getConnectResponse();
                if (response != null) {
                    log.info("Connected to server: {}", response);
                    isConnected = true;
                    responseLoadBalancer.setResponse(response);
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for response", e);
            }
        }
    }
}
