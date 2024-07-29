package project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import src.lib.Send;
import src.lib.TypeReceive;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

import src.lib.Send;
import org.project.ServerManager;

import project.Chat.ServerInfo;
import project.Chat.ServerManagerInfo;
import project.Chat.Database;
import project.Chat.Receive;

public class LoadBalancer extends Thread {
    private static int LOAD_BALANCER_PORT;
    private final boolean isClient;
    private static final int MAX_CLIENTS = 2;
    private static final int INITIAL_PORT = 1234;
    private int portDefault = INITIAL_PORT;
    private static ServerManagerUI ui;

    public LoadBalancer(int port, boolean isClient) {
        LOAD_BALANCER_PORT = port;
        this.isClient = isClient;
        Database.serverList = new ArrayList<>();

        try {
            initializeServerManager();
        } catch (IOException e) {
            e.printStackTrace();
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

    private void updateUI() {
        SwingUtilities.invokeLater(() -> {
            ui.updateServerList(Database.serverManagerInfoList);
        });
    }

    public static ServerManagerUI getUI() {
        return ui;
    }


    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(LOAD_BALANCER_PORT)) {
            System.out.println("Load balancer started on port " + LOAD_BALANCER_PORT);

            while (true) {
                if (isClient) {
                    handleClientLoad(serverSocket);
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
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

    private void handleClientWithServerManager(Socket clientSocket, ServerManagerInfo serverManagerInfo) {
        System.out.println("Handling client with server manager on port " + serverManagerInfo.getPort());
        try {
            serverManagerInfo.getServerManager().setIsRunning(true);
            Socket newServerSocket = new Socket("localhost", serverManagerInfo.getPort());
            ServerInfo newServer = new ServerInfo("localhost", serverManagerInfo.getPort(), newServerSocket);
            Database.serverList.add(newServer);
    
            // Start receiving data from the new server
            new Thread(new Receive(newServerSocket, newServer)).start();
            new Send(newServerSocket).sendData("type:load-balancer");
    
            // Handle the client connection with the newly started server
            newServer.incrementClients();
            handleClientConnection(clientSocket, newServer);

            updateUI();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to connect to the new server.");
        }
    }    

    private void handleClientConnection(Socket clientSocket, ServerInfo server) {
        new Receive(clientSocket, server).receiveData();
        try {
            new Send(clientSocket).sendData("type:server&&data:" + server.toString());
        } catch (IOException e) {
            e.printStackTrace();
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
                    e.printStackTrace();
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
            System.out.println("Failed to start and connect to the new server.");
        }
    }

    public static void main(String[] args) throws IOException {
        int clientport = 3005;
        boolean isClient = true;
        LoadBalancer loadBalancerClient = new LoadBalancer(clientPort, isClient);
        loadBalancerClient.start();
    }
}
