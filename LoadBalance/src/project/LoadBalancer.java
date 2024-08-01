package project;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import src.lib.Send;
import org.project.ServerManager;

import project.Chat.ServerInfo;
import project.Chat.ServerManagerInfo;
import project.Chat.Database;
import project.Chat.Receive;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class LoadBalancer {
    private static int LOAD_BALANCER_PORT;
    private static final int MAX_CLIENTS = 2;
    private static final int INITIAL_PORT = 1234;
    private int portDefault = INITIAL_PORT;

    public LoadBalancer() {
        Database.serverList = new ArrayList<>();
        Database.serverList.add(new ServerInfo("localhost", 1234, null));
        Database.serverList.add(new ServerInfo("localhost", 1235, null));

        


        // LOAD_BALANCER_PORT = port;
        // Database.serverList = new ArrayList<>();

        // try {
        //     initializeServerManager();
        // } catch (IOException e) {
        //     Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());

        // }
    }

    private void initializeServerManager() throws IOException {
        ServerManager serverManager = new ServerManager();
        serverManager.startServer(portDefault);

        ServerManagerInfo serverManagerInfo = new ServerManagerInfo(serverManager, portDefault);
        Database.oldServerManager.add(serverManagerInfo);

        Socket serverSocket = new Socket("localhost", portDefault);
        Database.serverList.add(new ServerInfo("localhost", portDefault, serverSocket));
        new Thread(new Receive(serverSocket, getAvailableServer())).start();
        new Send(serverSocket).sendData("type:load-balancer");
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(LOAD_BALANCER_PORT)) {
            System.out.println("Load balancer started on port " + LOAD_BALANCER_PORT);

            while (true) {
                handleClientLoad(serverSocket);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());

        }
    }

    private ServerInfo getAvailableServer() {
        return Database.serverList.stream()
                .filter(server -> server.getActiveClients() < MAX_CLIENTS)
                .findFirst()
                .orElse(null);
    }

    private void handleClientLoad(ServerSocket serverSocket) throws IOException {
        try (Socket clientSocket = serverSocket.accept()) {
            System.out.println("Client connected to load balancer....");
            Thread.sleep(2000);
            ServerInfo availableServer = getAvailableServer();

            if (availableServer != null) {
                availableServer.incrementClients();
                handleClientConnection(clientSocket, availableServer);
            } else {
                handleFullServers(clientSocket);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
        }
    }

    private void handleClientConnection(Socket clientSocket, ServerInfo server) {
        new Receive(clientSocket, server).receiveData();
        try {
            new Send(clientSocket).sendData("type:server&&data:" + server.toString());
        } catch (IOException e) {
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
        }
    }

    private void handleFullServers(Socket clientSocket) {
        System.out.println("All servers are full. Please try again later.");

        for (ServerManagerInfo serverManagerInfo : Database.oldServerManager) {
            if (!serverManagerInfo.getOpenning()) {
                serverManagerInfo.setOpenning(true);
                ServerManager serverManager = new ServerManager();
                serverManager.startServer(serverManagerInfo.getPort());
                serverManagerInfo.setServerManager(serverManager);

                try {
                    connectToNewServer(serverManagerInfo.getPort(), clientSocket);
                } catch (IOException e) {
                    Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());

                }
                return;
            }
        }

        startNewServer(clientSocket);
    }

    private void connectToNewServer(int port, Socket clientSocket) throws IOException {
        Socket newServerSocket = new Socket("localhost", port);
        ServerInfo newServer = new ServerInfo("localhost", port, newServerSocket);
        Database.serverList.add(newServer);

        new Thread(new Receive(newServerSocket, newServer)).start();
        new Send(newServerSocket).sendData("type:load-balancer");

        ServerInfo availableServer = getAvailableServer();
        if (availableServer != null) {
            availableServer.incrementClients();
            handleClientConnection(clientSocket, availableServer);
        } else {
            System.out.println("Unexpected error: No available server after creating a new one.");
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.WARNING, "Unexpected error: No available server after creating a new one.");
        }
    }

    private void startNewServer(Socket clientSocket) {
        portDefault++;

        ServerManager serverManager = new ServerManager();
        serverManager.startServer(portDefault);

        ServerManagerInfo serverManagerInfo = new ServerManagerInfo(serverManager, portDefault);
        Database.oldServerManager.add(serverManagerInfo);

        try {
            connectToNewServer(portDefault, clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to the new server.");
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE,
                    "Failed to start and connect to the new server: {0}", e.getMessage());

        }
    }

    public static void main(String[] args) {
        LoadBalancer loadBalancer = new LoadBalancer();
        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            // Define a context that listens for requests
            server.createContext("/", new MyHandler());

            // Start the server
            server.setExecutor(null);
            server.start();
            System.out.println("LoadBalancer is running on http://localhost:8080");
        } catch (IOException e) {
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
        }
    }
    
    // Handler that processes incoming requests
    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Read the request from the client
            ServerInfo serverEmpty = Database.serverList.stream()
                .filter(server -> server.getActiveClients() < MAX_CLIENTS)
                .findFirst()
                .orElse(null);
            String response = "type:server&&data:" + serverEmpty.toString();
            

            // Set the response headers and status code
            exchange.sendResponseHeaders(200, response.length());

            try ( // Write the response body
                    OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            } catch (IOException e) {
                Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
            }
        }
    }
}

