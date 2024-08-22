package com.example.loadbalance.payloads;

public class ClientInfo {
    private String name;
    private String serverInfo;

    public ClientInfo(String name, String serverinfo) {
        this.name = name;
        this.serverInfo = serverinfo;
    }

    public String getName() {
        return name;
    }

    public String getServerInfo() {
        return serverInfo;
    }
}