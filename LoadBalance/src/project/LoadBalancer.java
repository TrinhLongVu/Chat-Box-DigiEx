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

import javax.xml.crypto.Data;

import src.lib.Send;
import src.lib.Helper;
import src.lib.TypeReceive;
import org.project.ServerManager;

class Database {
    public static List<ClientInfo> clients = new ArrayList<>();
    public static List<ServerInfo> serverList = new ArrayList<>();
}

public class LoadBalancer extends Thread {
    private static int LOAD_BALANCER_PORT;
    private boolean isClient = true;
    private final int MAX_CLIENTS = 1;
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
        new Send(clientSocket).sendData("type:server&&data:" + server.toString());
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

class ServerInfo {
    private String host;
    private int port;
    private int activeClients;
    private Socket socket;

    public ServerInfo(String host, int port, Socket ss) {
        this.host = host;
        this.socket = ss;
        this.port = port;
        this.activeClients = 0;
    }

    public String getHost() {
        return host;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getPort() {
        return port;
    }

    public synchronized int getActiveClients() {
        return activeClients;
    }

    public synchronized void incrementClients() {
        activeClients++;
    }

    public synchronized void decrementClients() {
        activeClients--;
    }

    @Override
    public String toString() {
        return this.host + "@" + this.port;
    }
}

class ClientInfo {
    private String name;
    private String serverinfo;

    public ClientInfo(String name, String serverinfo) {
        this.name = name;
        this.serverinfo = serverinfo;
    }

    public String getName() {
        return name;
    }

    public String getServerinfo() {
        return serverinfo;
    }
}

class Receive implements Runnable{
    private String receiveMsg = "";
    private BufferedReader br;
    private Socket socket;
    private ServerInfo availableServer;

    public Receive(Socket ss, ServerInfo availableServer) {
        this.availableServer = availableServer;
        this.socket = ss;
        InputStream is;
        try {
            is = ss.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void receiveData() {
        try {
            this.receiveMsg = this.br.readLine();
            if (receiveMsg != null) {
                System.out.println("Received: " + receiveMsg);
                TypeReceive data = Helper.FormatData(receiveMsg);

                switch (data.getType()) {
                    case "login": {
                        
                        Database.clients.add(new ClientInfo(data.getNameSend(), availableServer.toString()));
                        updateUserOnline();
                        return;
                    }
                    default:
                        System.out.println("Received invalid data: " + receiveMsg);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateUserOnline() {
        for (ServerInfo server : Database.serverList) {
            List<String> names = Database.clients.stream()
                    .map(ClientInfo::getName)
                    .collect(Collectors.toList());

            new Send(server.getSocket()).sendData("type:users&&data:" + names.toString());
        }
    }

    public void run() {
        String receiveMsg;
        try {
            while ((receiveMsg = br.readLine()) != null) {
                System.out.println("mes  sage::::" + receiveMsg);
                TypeReceive data = Helper.FormatData(receiveMsg);

                if (data == null) {
                    System.out.println("Received invalid data: " + receiveMsg);
                    continue;
                }

                switch (data.getType()) {
                    case "chat": {
                        for (ClientInfo client : Database.clients) {
                            if (client.getName().equals(data.getNameReceive())) {
                                for (ServerInfo server : Database.serverList) {
                                    if (client.getServerinfo().equals(server.toString())) {
                                        new Send(server.getSocket()).sendData(receiveMsg);
                                    }
                                }
                            }
                        }
                        break;
                    }

                    case "disconnect": {
                        Iterator<ClientInfo> iterator = Database.clients.iterator();
                        while (iterator.hasNext()) {
                            ClientInfo client = iterator.next();
                            if (client.getName().equals(data.getNameSend())) {
                                for (ServerInfo server : Database.serverList) {
                                    if (client.getServerinfo().equals(server.toString())) {
                                        server.decrementClients();
                                    }
                                }
                                iterator.remove(); // Safe removal
                            }
                        }
                        updateUserOnline();
                    }
                    
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from socket: " + e.getMessage());
        }
    }
}
