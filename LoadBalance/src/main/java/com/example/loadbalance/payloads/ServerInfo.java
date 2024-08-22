package com.example.loadbalance.payloads;
import java.net.Socket;

public class ServerInfo {
    private String host;
    private int port;
    private int activeClients;
    private int serverSize;
    private Socket socket;

    public ServerInfo(String host, int port, Socket ss, int serverSize) {
        this.host = host;
        this.socket = ss;
        this.port = port;
        this.serverSize = serverSize;
        this.activeClients = 0;
    }

    public String getHost() {
        return host;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getServerSize() {
        return serverSize;
    }
    
    public void setServerSize(int serverSize) {
        this.serverSize = serverSize;
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