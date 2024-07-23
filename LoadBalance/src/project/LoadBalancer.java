package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.xml.crypto.Data;

import src.lib.Send;
import src.lib.Helper;
import src.lib.TypeReceive;

public class LoadBalancer {
    private static final int LOAD_BALANCER_PORT = 3005;
    private List<ServerInfo> serverList;

    public LoadBalancer() {
        serverList = new ArrayList<>();
        serverList.add(new ServerInfo("localhost", 1234));
        serverList.add(new ServerInfo("192.168.0.60", 3005));
        serverList.add(new ServerInfo("localhost", 5002));
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(LOAD_BALANCER_PORT);
        System.out.println("Load balancer started on port " + LOAD_BALANCER_PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected to load balancer");

            ServerInfo availableServer = getAvailableServer();
            if (availableServer != null) {
                // Redirect client to the available server
                // redirectClient(clientSocket, availableServer);
                // handle and close connection login....
                // 
                System.out.println(availableServer.toString());
                availableServer.incrementClients();
                // new Receive(clientSocket).start(); 
                new Receive(clientSocket, availableServer).receiveData();
                new Send(clientSocket).sendData("type:server" + "&&" + "data:" + availableServer.toString());
                clientSocket.close();
            } else {
                System.out.println("All servers are full. Please try again later.");
                // send message for users....
                clientSocket.close();
            }
        }
    }

    private ServerInfo getAvailableServer() {
        for (ServerInfo server : serverList) {
            if (server.getActiveClients() < 2) {
                return server;
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.start();
    }
}

class ServerInfo {
    private String host;
    private int port;
    private int activeClients;

    public ServerInfo(String host, int port) {
        this.host = host;
        this.port = port;
        this.activeClients = 0;
    }

    public String getHost() {
        return host;
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

class Database {
    public static List<ClientInfo> clients = new ArrayList<>();
}

class ServerCommunication implements Runnable {
    private Socket inputSocket;
    private Socket outputSocket;
    private ServerInfo serverInfo;

    public ServerCommunication(Socket inputSocket, Socket outputSocket, ServerInfo serverInfo) {
        this.inputSocket = inputSocket;
        this.outputSocket = outputSocket;
        this.serverInfo = serverInfo;
    }

    @Override
    public void run() {
        try {
            InputStream in = inputSocket.getInputStream();
            OutputStream out = outputSocket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputSocket.close();
                outputSocket.close();
                serverInfo.decrementClients();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class Receive{
    private String receiveMsg = "";
    private BufferedReader br;
    private Socket socket;
    private ServerInfo availableServer;

    public Receive(Socket ss, ServerInfo availableServer) {
        this.socket = ss;
        this.availableServer = availableServer;
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

                switch (data.getType()){
                    case "login": {
                        Database.clients.add(new ClientInfo(data.getNameSend(), availableServer.toString()));
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
}
