package com.example.client.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.example.client.chat.SocketManager;
import com.example.client.core.ClientInfo;
import com.example.support.Helper;
import com.example.support.Send;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoadBalanceManager {
    private static final Logger log = LogManager.getLogger(LoadBalanceManager.class);
    private final SocketManager socketManager;
    private final ClientInfo clientInfo;

    private String SCHEME = "http://";
    private String LOADBALANCER_HOST = "localhost";
    private String LOADBALANCER_PORT = "8080";

    private String LOADBALANCER_URL = SCHEME + LOADBALANCER_HOST + ":" + LOADBALANCER_PORT;

    public String getConnectResponse() {
        StringBuilder content = new StringBuilder();

        try {
            // URL of the LoadBalancer
            URL url = new URL(LOADBALANCER_URL + "/connect");

            // Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(false);
            conn.setRequestProperty("Content-Type", "text/plain");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
        } catch (IOException e) {
            log.error("Failed to handle request from server: {}", e.getMessage());
        }

        return content.toString();
    }

    public void notifyConnected(String host, int port, String name) {
        try {
            URL loadBalancerUrl = new URL(LOADBALANCER_URL + "/login");

            HttpURLConnection loadBalancerConn = (HttpURLConnection) loadBalancerUrl.openConnection();
            loadBalancerConn.setRequestMethod("POST");
            loadBalancerConn.setDoOutput(true);
            loadBalancerConn.setRequestProperty("Content-Type", "text/plain");

            // Message indicating successful connection to the server
            String confirmationMessage = name + "&&" + host + "@"+ port;
            try (OutputStream os = loadBalancerConn.getOutputStream()) {
                os.write(confirmationMessage.getBytes());
                os.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(loadBalancerConn.getInputStream()));
            StringBuilder newContent = new StringBuilder();

            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                newContent.append(inputLine);
            }

            // Close connections

            in.close();
        } catch (IOException e) {
            log.error("Error sending confirmation to LoadBalancer: {}", e.getMessage());
        }
    }

    public void reconnectServerResponse() {
        boolean isRunning = false;
        while (!isRunning) {

            Socket newSocket;
            StringBuilder content = new StringBuilder();
            try {
                Thread.sleep(4000);
                URL url = new URL(LOADBALANCER_URL + "/reconnect");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "text/plain");

                String confirmationMessage = clientInfo.getUserName() + "&&" + "localhost@" + socketManager.getSocket().getPort();

                OutputStream os = conn.getOutputStream();
                os.write(confirmationMessage.getBytes());
                os.flush();

                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                }
                System.out.println(content.toString());
                String data = Helper.formatData(content.toString()).getData();
                if (data.equals("null")) {
                    log.error("No server available");
                } else {
                    String[] hostAndPort = data.split("@");
                    String host = hostAndPort[0];
                    int port = Integer.parseInt(hostAndPort[1]);

                    newSocket = new Socket(host, port);

                    try {
                        if (socketManager.getSocket() != null && !socketManager.getSocket().isClosed()) {
                            socketManager.getSocket().close();
                        }

                        // Reconnect to new server
                        if(newSocket.isConnected()) {
                            log.info("Reconnected to server: {}:{}", host, port);
                            socketManager.setSocket(newSocket);
                            notifyConnected(host, port, clientInfo.getUserName());
                        }
                    } catch (IOException e) {
                        log.error("Error closing client socket: {}", e.getMessage());
                    }

                    new Send(newSocket).sendData("type:login&&send:" + clientInfo.getUserName());
                    isRunning = true;
                }
            } catch (IOException e) {
                log.error("Failed to reconnect to server: {}", e.getMessage());
                JOptionPane.showMessageDialog(null, "An error occurred while reconnecting to the server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (InterruptedException e) {
                log.error("Interrupted while reconnecting to the server: {}", e.getMessage());
            }
        }
    }
}