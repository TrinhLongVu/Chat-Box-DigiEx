package project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

import src.lib.Send;
import org.project.ServerManager;

import project.Chat.ServerInfo;
import project.Chat.Database;
import project.Chat.Receive;

public class LoadBalancer extends Thread {
    private static int LOAD_BALANCER_PORT;
    private boolean isClient = true;
    private final int MAX_CLIENTS = 2;
    private int PORT_DEFAULT = 1234;
    private static ServerManagerUI ui;

    public LoadBalancer(int port, boolean isClient) {
        LOAD_BALANCER_PORT = port;
        this.isClient = isClient;
        Database.serverList = new CopyOnWriteArrayList<>();

        SwingUtilities.invokeLater(() -> {
            ui = new ServerManagerUI();
            ui.setVisible(true);
            updateUI();
        });

        try {
            ServerManager serverManager = new ServerManager();
            serverManager.startServer(PORT_DEFAULT);
            serverManager.setIsRunning(true);
            Socket server = new Socket("localhost", PORT_DEFAULT);
            Database.serverList.add(new ServerInfo("localhost", PORT_DEFAULT, server));
            ServerManagerInfo serverManagerInfo = new ServerManagerInfo(PORT_DEFAULT, serverManager);
            serverManagerInfo.setIsRunning(true);
            Database.serverManagerInfoList.add(serverManagerInfo);
            System.out.println("Status when created: " + serverManagerInfo.getServerManager().isRunning());
            new Thread(new Receive(server, getAvailableServer())).start();
            new Send(server).sendData("type:load-balancer");
            System.out.println("Server started on port " + PORT_DEFAULT + ". Status: " + serverManager.isRunning());

            // Update the UI with the initial server list
            updateUI();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        try {
            ServerSocket serverSocket = new ServerSocket(LOAD_BALANCER_PORT);
            System.out.println("Load balancer started on port " + LOAD_BALANCER_PORT);
            System.out.println("--------------------------------------------");
    
            while (true) {
                if (isClient) {
                    handleClientLoad(serverSocket);
                } else {
                    System.out.println("Waiting for client...");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error::::" + e.getMessage());
        }
    }

    private ServerInfo getAvailableServer() {
        for (ServerInfo server : Database.serverList) {
            System.out.println("Checking server on port " + server.getPort() + " with " + server.getActiveClients() + " active clients.");
            if (server.getActiveClients() < MAX_CLIENTS) {
                return server;
            }
        }
        return null;
    }

    private void handleClientLoad(ServerSocket serverSocket) throws IOException {
        System.out.println("--------------------------------------------");
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected to load balancer");

        ServerInfo availableServer = getAvailableServer();
    
        if (availableServer != null) {
            availableServer.incrementClients();
            handleClientConnection(clientSocket, availableServer);
            System.out.println("--------------------------------------------");
            System.out.println("New client connected to server " + availableServer.getPort());
            System.out.println("--------------------------------------------");
        } else {

            System.out.println("--------------------------------------------");

            System.out.println("No available server found. Checking for free server manager...");
            ServerManagerInfo serverManagerInfo = getServerManagerInfoFree();
            if (serverManagerInfo == null) {
                System.out.println("No free server manager found. Starting new server...");
                handleFullServers(clientSocket);
            } else {
                System.out.println("Found a free server manager. Handling client...");
                handleClientWithServerManager(clientSocket, serverManagerInfo);
            }

            System.out.println("--------------------------------------------");

        }
        clientSocket.close();
        System.out.println("--------------------------------------------");
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
        System.out.println("All servers are full. Starting a new server...");
    
        // Increment the port for the new server
        PORT_DEFAULT++;
        int newServerPort = PORT_DEFAULT;
        ServerManager newServerManager = new ServerManager();
        newServerManager.startServer(newServerPort);
        newServerManager.setIsRunning(true);
        ServerManagerInfo newServerManagerInfo = new ServerManagerInfo(newServerPort, newServerManager);
        newServerManagerInfo.isRunning = true;
    
        try {
            // Start the new server
            Database.serverManagerInfoList.add(newServerManagerInfo);

            Thread.sleep(1000);
    
            // Connect to the new server
            Socket newServerSocket = new Socket("localhost", newServerPort);
            ServerInfo newServer = new ServerInfo("localhost", newServerPort, newServerSocket);
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
    
            System.out.println("New server started on port " + newServerPort);

            updateUI();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Failed to start and connect to the new server.");
        }
    }
    
    public ServerManagerInfo getServerManagerInfoFree() {
        System.out.println("Checking for free server manager...");
        for (ServerManagerInfo serverManagerInfo : Database.serverManagerInfoList) {
            if (!serverManagerInfo.getServerManager().isRunning()) {
                System.out.println("Found free server manager on port " + serverManagerInfo.getPort());
                return serverManagerInfo;
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        int clientport = 3005;
        boolean isClient = true;
        LoadBalancer loadBalancerClient = new LoadBalancer(clientport, isClient);
        loadBalancerClient.start();
    }
}
