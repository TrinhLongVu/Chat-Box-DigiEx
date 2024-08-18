package com.example.Broker.service;

import org.apache.catalina.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class ServerMonitor implements Runnable {
    private final String SERVER_TOPIC = "Server";
    private final MessageBroker messageBroker;

    @Autowired
    public ServerMonitor(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    @Override
    public void run() {
        while (true) {
            Iterator<Socket> iterator = messageBroker.getSocketsByKey(SERVER_TOPIC).iterator();

            while (iterator.hasNext()) {
                Socket serverSocket = iterator.next();
                try {
                    serverSocket.getOutputStream().write(0);
                } catch (IOException e) {
                    removeDisconnectedServer(serverSocket);
                }
            }

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void removeDisconnectedServer(Socket serverSocket) {
        try {
            List<Socket> sockets = messageBroker.getSocketsByKey(SERVER_TOPIC);
            if (sockets != null) {
                sockets.remove(serverSocket);
                if (sockets.isEmpty()) {
                    messageBroker.getConnectedServers().remove(SERVER_TOPIC);
                }
            }
            serverSocket.close();
        } catch (IOException e) {
            Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error closing server socket: {0}",
                    e.getMessage());
        }
    }
}
