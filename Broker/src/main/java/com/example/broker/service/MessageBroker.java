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
import org.springframework.scheduling.annotation.Scheduled;


@Component
@RequiredArgsConstructor
public class MessageBroker {
    private ConcurrentHashMap<String, List<Socket>> connectedServers = new ConcurrentHashMap<>();
    private static final Logger log = LogManager.getLogger(MessageBroker.class);
    private final ServerSocketHandler serverSocketHandler;
    private static final String SERVER_TOPIC = "Server";
    public int PORT = 4000;


    @Bean
    public void init() {
        log.info("MessageBroker is starting...");
        try {
            serverSocketHandler.setServerSocket(new ServerSocket(PORT));
        } catch (IOException e) {
            log.error("Error starting server: {}", e.getMessage());
        }
    }

    @Scheduled(fixedRate = 300)
    private void allowNewServerConnection() {
        try {
            Socket serverSocket = serverSocketHandler.getServerSocket().accept();
            connectedServers.computeIfAbsent(SERVER_TOPIC, _ -> new CopyOnWriteArrayList<>()).add(serverSocket);

            new Thread(new ServerHandler(this, serverSocket)).start();
        } catch (IOException e) {
            log.error("Error accepting connection: {}", e.getMessage());
        }
    }

    public List<Socket> getConnectedServersByKey(String key) {
        return connectedServers.getOrDefault(key, new ArrayList<>());
    }
}
