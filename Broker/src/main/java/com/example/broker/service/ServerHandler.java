package com.example.broker.service;

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.net.SocketException;
import com.example.support.Send;
import java.io.InputStreamReader;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class ServerHandler implements Runnable {
    private static final Logger log = LogManager.getLogger(ServerHandler.class);
    private static final String SERVER_TOPIC = "Server";
    private final MessageBroker messageBroker;
    private BufferedReader bufferedReader;

    @Override
    public void run() {
        try {
            String message;
            while ((message = bufferedReader.readLine()) != null) {
                broadcastMessage(message);
            }
        } catch (SocketException e) {
            log.error("Server suddenly closed {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error reading from server: {}", e.getMessage());
        }
    }

    public ServerHandler(MessageBroker messageBroker, Socket serverSocket) {
        log.info("{} {} connected", SERVER_TOPIC, serverSocket.getPort());
        this.messageBroker = messageBroker;
        InputStream is;
        try {
            is = serverSocket.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            log.error("Problem with handling new server: {}", e.getMessage());
        }
    }
    
    private void broadcastMessage(String message) {
        for (Socket serverDestination : messageBroker.getConnectedServersByKey(SERVER_TOPIC)) {
            try {
                log.info("Broadcast message: {}", message);
                new Send(serverDestination).sendData(message);
                log.info("Destination {}", serverDestination);
            } catch (IOException e) {
                log.error("Error broadcasting message to server {} {}", serverDestination, e.getMessage());
            }
        }
    }
}