package com.example.Broker.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CopyOnWriteArrayList;

import com.example.Support.Send;
import org.springframework.stereotype.Component;

@Component
public class MessageBroker {
    private final String SERVER_TOPIC = "Server";
    public static final int PORT = 4000;

    private HashMap<String, List<Socket>> connectedServers = new HashMap<>();

    public MessageBroker() {
        startMessageBroker(PORT);
    }

    public void startMessageBroker(int port) {
        Logger.getLogger(MessageBroker.class.getName()).log(Level.INFO, "Message Broker is running....");
        ServerSocket brokerSocket;
        try {
            brokerSocket = new ServerSocket(port);

//            // Start server monitor to determine if server is still connected
//            new Thread(new ServerMonitor()).start();
            while (true) {
                allowNewServerConnection(brokerSocket);
            }
        } catch (IOException e) {
            Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error starting server: {0}",
                    e.getMessage());
        }
    }

    private void allowNewServerConnection(ServerSocket brokerSocket) {
        try {
            Socket serverSocket = brokerSocket.accept();
            connectedServers.computeIfAbsent(SERVER_TOPIC, k -> new CopyOnWriteArrayList<>()).add(serverSocket);

            new Thread(new ServerHandler(serverSocket)).start();
        } catch (IOException e) {
            Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error accepting connection: {0}",
                    e.getMessage());
        }
    }

    public List<Socket> getSocketsByKey(String key) {
        return connectedServers.getOrDefault(key, new ArrayList<>());
    }

    public HashMap<String, List<Socket>> getConnectedServers() {
        return connectedServers;
    }

    private class ServerHandler implements Runnable {
        private Socket serverSocket;
        private BufferedReader br;

        @Override
        public void run() {
            try {
                String message;
                while ((message = br.readLine()) != null) {
                    broadcastMessage(message);
                }
            } catch (Exception e) {
                Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error reading from server: {0}",
                        e.getMessage());
            }
        }

        public ServerHandler(Socket serverSocket) {
            this.serverSocket = serverSocket;
            InputStream is;
            try {
                is = this.serverSocket.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
            } catch (IOException e) {
                Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE,
                        "Problem with handling new server: {0}", e.getMessage());
            }
        }

        private void broadcastMessage(String message) {
            for (Socket serverDestination : getSocketsByKey(SERVER_TOPIC)) {
                try {
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.INFO, "Message broadcasted: {0}",
                            message);
                    new Send(serverDestination).sendData(message);
                } catch (IOException e) {
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error broadcasting message: {0}",
                            e.getMessage());
                }
            }
        }
    }


}
