package com.example.broker.service;

import com.example.support.Send;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerHandler implements Runnable {
    private static final Logger log = LogManager.getLogger(ServerHandler.class);
    private static final String SERVER_TOPIC = "Server";
    private final MessageBroker messageBroker;
    private BufferedReader br;

    @Override
    public void run() {
        try {
            String message;
            while ((message = br.readLine()) != null) {
                broadcastMessage(message);
            }
        } catch (Exception e) {
            log.error("Error reading from server: {}",
                    e.getMessage());
        }
    }

    public ServerHandler(MessageBroker messageBroker, Socket serverSocket) {
        log.info("{} {} connected", SERVER_TOPIC, serverSocket.getPort());
        this.messageBroker = messageBroker;
        InputStream is;
        try {
            is = serverSocket.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            log.error("Problem with handling new server: {}", e.getMessage());
        }
    }
    
    private void broadcastMessage(String message) {
        for (Socket serverDestination : messageBroker.getSocketsByKey(SERVER_TOPIC)) {
            try {
                log.info("Broadcast message: {}",
                        message);
                new Send(serverDestination).sendData(message);
            } catch (IOException e) {
                log.error("Error broadcasting message: {}",
                        e.getMessage());
            }
        }
    }
}