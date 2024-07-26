package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


import src.lib.Send;
import src.lib.Helper;
import src.lib.TypeReceive;
import org.project.ServerManager;

import project.Chat.ServerInfo;
import project.Chat.Database;
import project.Chat.ClientInfo;
import project.Chat.Receive;



public class LoadBalancer extends Thread {
    private static int LOAD_BALANCER_PORT;
    private boolean isClient = true;
    private final int MAX_CLIENTS = 2;
    private int PORT_DEFAULT = 1234;

    public LoadBalancer(int port, boolean isClient) {
        LOAD_BALANCER_PORT = port;
        this.isClient = isClient;
        Database.serverList = new ArrayList<>();
        try {
            new ServerManager().startServer(PORT_DEFAULT);
            Socket server = new Socket("localhost", PORT_DEFAULT);
            Database.serverList.add(new ServerInfo("localhost", PORT_DEFAULT, server));
            new Thread(new Receive(server, getAvailableServer())).start();
            new Send(server).sendData("type:load-balancer");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(LOAD_BALANCER_PORT);
            System.out.println("Load balancer started on port " + LOAD_BALANCER_PORT);
    
            while (true) {
                if (isClient) {
                    hanleClientLoad(serverSocket);
                }
            }
        } catch (IOException e) {
            System.out.println("error::::" + e.getMessage());
        }
       
    }

    private ServerInfo getAvailableServer() {
        for (ServerInfo server : Database.serverList) {
            if (server.getActiveClients() < MAX_CLIENTS) {
                return server;
            }
        }
        return null;
    }

    private void hanleClientLoad(ServerSocket serverSocket) throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected to load balancer");
        ServerInfo availableServer = getAvailableServer();

        if (availableServer != null) {
            availableServer.incrementClients();
            handleClientConnection(clientSocket, availableServer);
        } else {
            handleFullServers(clientSocket);
        }
        clientSocket.close();
    }

    private void handleClientConnection(Socket clientSocket, ServerInfo server) {
        new Receive(clientSocket, server).receiveData();
        
        try {
            new Send(clientSocket).sendData("type:server&&data:" + server.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Method to handle the case when all servers are full
    private void handleFullServers(Socket clientSocket) {
        System.out.println("All servers are full. Please try again later.");

        // Start a new server
        PORT_DEFAULT++;
        new ServerManager().startServer(PORT_DEFAULT);

        // Connect to the new server
        try {
            Socket newServerSocket = new Socket("localhost", PORT_DEFAULT);
            ServerInfo newServer = new ServerInfo("localhost", PORT_DEFAULT, newServerSocket);
            Database.serverList.add(newServer);

            // Start receiving data from the new server
            new Thread(new Receive(newServerSocket, newServer)).start();
            new Send(newServerSocket).sendData("type:load-balancer");

            // Handle the client connection with the newly started server
            ServerInfo availableServer = getAvailableServer();
            if (availableServer != null) {
                availableServer.incrementClients();
                handleClientConnection(clientSocket, availableServer);
            } else {
                // Should not reach here if handled correctly
                System.out.println("Unexpected error: No available server after creating a new one.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to the new server.");
        }
    }
    
    public static void main(String[] args) throws IOException {
        int clientport = 3005;
        boolean isClient = true;
        LoadBalancer loadBalancerClient = new LoadBalancer(clientport, isClient);
        loadBalancerClient.start();
    }
}