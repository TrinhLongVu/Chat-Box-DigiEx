package project;

import java.io.IOException;
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

public class LoadBalancer extends Thread {
    private static int LOAD_BALANCER_PORT;
    private static final int MAX_CLIENTS = 2;
    private static final int INITIAL_PORT = 1234;
    private int portDefault = INITIAL_PORT;

    public LoadBalancer(int port) {
        LOAD_BALANCER_PORT = port;
        Database.serverList = new ArrayList<>();

        try {
            initializeServerManager();
        } catch (IOException e) {
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());

        }
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

    @Override
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
            System.out.println("Client connected to load balancer");
            ServerInfo availableServer = getAvailableServer();

            if (availableServer != null) {
                availableServer.incrementClients();
                handleClientConnection(clientSocket, availableServer);
            } else {
                handleFullServers(clientSocket);
            }
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
            System.out.println("Failed to connect to the new server.");
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE,
                    "Failed to start and connect to the new server: {0}", e.getMessage());

        }
    }

    public static void main(String[] args) {
        int clientPort = 3005;
        LoadBalancer loadBalancerClient = new LoadBalancer(clientPort);
        loadBalancerClient.start();
    }
}
