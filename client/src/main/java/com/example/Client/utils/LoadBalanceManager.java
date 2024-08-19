package com.example.Client.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.example.Client.chat.MessageManager;
import com.example.Client.chat.SocketManager;
import com.example.Client.view.HomePage;
import com.example.Client.view.LoginForm;
import com.example.Support.Helper;
import com.example.Support.Send;

public class LoadBalanceManager {
    private static final String LOAD_BALANCER_URL = "http://localhost:8080";

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
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, "Failed to handle request from server: {0}", e.getMessage());
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
            Logger.getLogger(LoginForm.class.getName()).log(Level.SEVERE, "Error sending confirmation to LoadBalancer: {0}",
                e.getMessage());
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
                Logger.getLogger(MessageManager.class.getName()).log(Level.INFO, "No server available");
                JOptionPane.showMessageDialog(null, "No server available", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return null;
            }

            String[] hostAndPort = data.split("@");
            String host = "localhost";
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
                Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, "Error closing client socket: {0}",
                        e.getMessage());
            }

            new Send(newSocket).sendData("type:login&&send:" + HomePage.myName);
        } catch (IOException e) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, "Failed to reconnect to server: {0}", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred while reconnecting to the server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return newSocket;
    }
}
