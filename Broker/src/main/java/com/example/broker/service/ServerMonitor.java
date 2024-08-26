package com.example.broker.service;

import java.util.List;
import java.net.Socket;
import org.slf4j.Logger;
import java.io.IOException;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;


@Component
@RequiredArgsConstructor
public class ServerMonitor {
    private static final Logger log = LoggerFactory.getLogger(ServerMonitor.class);
    private static final String SERVER_TOPIC = "Server";
    private final MessageBroker messageBroker;

    @Scheduled(fixedRate = 300)
    public void run() {
        for (Socket serverSocket : messageBroker.getConnectedServersByKey(SERVER_TOPIC)) {
            try {
                serverSocket.getOutputStream().write(0);
            } catch (IOException e) {
                log.info("Remove Server From Broadcast: {}", serverSocket);
                removeDisconnectedServer(serverSocket);
            }
        }
    }

    public void removeDisconnectedServer(Socket serverSocket) {
        try {
            List<Socket> sockets = messageBroker.getConnectedServersByKey(SERVER_TOPIC);
            if (sockets != null && !sockets.isEmpty()) {
                messageBroker.getConnectedServersByKey(SERVER_TOPIC).remove(serverSocket);
            }
            serverSocket.close();
        } catch (IOException e) {
            log.error("Error closing server socket: {}", e.getMessage());
        }
    }
}
