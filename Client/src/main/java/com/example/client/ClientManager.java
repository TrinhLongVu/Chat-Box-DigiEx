package com.example.client;

import com.example.client.core.ResponseLoadBalancer;
import com.example.client.utils.LoadBalanceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClientManager {
    private final ResponseLoadBalancer responseLoadBalancer;
    private final LoadBalanceManager loadBalanceManager;

    @Bean
    public void connectToServer() {
        String response;
        while(!responseLoadBalancer.isRunning()) {
            response = loadBalanceManager.getConnectResponse();
            if (response != null) {
                responseLoadBalancer.setResponse(response);
            }
        }
    }
}
