package com.example.broker.service;

import java.util.List;
import java.net.Socket;
import java.util.ArrayList;
import java.io.IOException;
import java.net.ServerSocket;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

@Component
@RequiredArgsConstructor
public class MessageBroker {
    private ConcurrentHashMap<String, List<Socket>> connectedServers = new ConcurrentHashMap<>();
    private static final Logger log = LogManager.getLogger(MessageBroker.class);
    private final BrokerSocketHandler brokerSocketHandler;
    private static final String SERVER_TOPIC = "Server";

    @Value("${broker.port}")
    public int brokerPort;


    @Bean
    public void init() {
        log.info("MessageBroker is starting...");
        try {
            brokerSocketHandler.setBrokerSocket(new ServerSocket(brokerPort));
        } catch (IOException e) {
            log.error("Error starting server: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300)
    private void allowNewServerConnection() {
        try {
            Socket serverSocket = brokerSocketHandler.getBrokerSocket().accept();

            // Update value of key present in the map, if the key is not present then add the key and value to the map
            connectedServers.computeIfAbsent(SERVER_TOPIC, _ -> new CopyOnWriteArrayList<>()).add(serverSocket);

            new Thread(new ServerHandler(this, serverSocket)).start();
        } catch (IOException e) {
            log.error("Error accepting connection from server: {}", e.getMessage());
        }
    }

    public List<Socket> getConnectedServersByKey(String key) {
        return connectedServers.getOrDefault(key, new ArrayList<>());
    }
}
