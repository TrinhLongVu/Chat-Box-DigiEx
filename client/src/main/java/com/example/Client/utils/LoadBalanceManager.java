package com.example.client.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;


import javax.swing.JOptionPane;

import com.example.client.chat.MessageManager;
import com.example.client.chat.SocketManager;
import com.example.client.view.HomePage;
import com.example.support.Helper;
import com.example.support.Send;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

public class LoadBalanceManager {
    private static final Logger log = LogManager.getLogger(LoadBalanceManager.class);
    private static String LOADBALANCER_HOST = "localhost";
    private static String LOADBALANCER_PORT = "8080";

    private static final String LOAD_BALANCER_URL = "http://" + LOADBALANCER_HOST + ":" + LOADBALANCER_PORT;

    @PostConstruct
    public String getConnectResponse() {
        StringBuilder content = new StringBuilder();

        try {
            // URL of the LoadBalancer
            URL url = new URL(LOAD_BALANCER_URL + "/connect");

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
            URL loadBalancerUrl = new URL(LOAD_BALANCER_URL + "/login");

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

    public Socket reconnectServerResponse() {
        Socket newSocket = null;
        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL(LOAD_BALANCER_URL + "/connect");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            }
            String data = Helper.formatData(content.toString()).getData();
            
            if (data.equals("null")) {
                log.error("No server available");
                JOptionPane.showMessageDialog(null, "No server available", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return null;
            }

            String[] hostAndPort = data.split("@");
            String host = hostAndPort[0];
            int port = Integer.parseInt(hostAndPort[1]);

            newSocket = new Socket(host, port);

            try {
                if (SocketManager.getSocket() != null && !SocketManager.getSocket().isClosed()) {
                    SocketManager.getSocket().close();
                }
                
                // Reconnect to new server
                SocketManager.setSocket(newSocket);
                new MessageManager(SocketManager.getSocket()).start();
            } catch (IOException e) {
                log.error("Error closing client socket: {}", e.getMessage());
            }

            new Send(newSocket).sendData("type:login&&send:" + HomePage.myName);
        } catch (IOException e) {
            log.error("Failed to reconnect to server: {}", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred while reconnecting to the server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return newSocket;
    }
}
