package com.example.broker.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class MessageBroker {
    private static final Logger log = LogManager.getLogger(MessageBroker.class);
    private static final String SERVER_TOPIC = "Server";
    public static final int PORT = 4000;

    private ConcurrentHashMap<String, List<Socket>> connectedServers = new ConcurrentHashMap<>();

    public MessageBroker() {
        startMessageBroker(PORT);
    }

    public void startMessageBroker(int port) {
        log.info("MessageBroker is starting...");
        ServerSocket brokerSocket;
        try {
            brokerSocket = new ServerSocket(port);
            
            while(true) {
                allowNewServerConnection(brokerSocket);
            }
        } catch (IOException e) {
            log.error("Error starting server: {}", e.getMessage());
        }
    }

    private void allowNewServerConnection(ServerSocket brokerSocket) {
        try {
            Socket serverSocket = brokerSocket.accept();
            connectedServers.computeIfAbsent(SERVER_TOPIC, k -> new CopyOnWriteArrayList<>()).add(serverSocket);

            new Thread(new ServerHandler(this, serverSocket)).start();
        } catch (IOException e) {
            log.error("Error accepting connection: {}",
                    e.getMessage());
        }
    }

    public List<Socket> getSocketsByKey(String key) {
        return connectedServers.getOrDefault(key, new ArrayList<>());
    }

    public ConcurrentMap<String, List<Socket>> getConnectedServers() {
        return connectedServers;
    }
}
