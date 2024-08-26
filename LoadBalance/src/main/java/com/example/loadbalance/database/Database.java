package com.example.loadbalance.database;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.example.loadbalance.controllers.ServerController;
import com.example.loadbalance.payloads.ClientInfo;
import com.example.loadbalance.payloads.ServerInfo;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class Database {
    private List<ClientInfo> clients ;
    private List<ServerInfo> serverList;
    private static final Logger log = LogManager.getLogger(Database.class);
    public Database(){
        this.clients = new ArrayList<>();
        this.serverList = new ArrayList<>();
    }

    public ServerInfo getServerInfo(String host, int port) {
        return serverList.stream()
                .filter(server -> server.getHost().equals(host) && server.getPort() == port)
                .findFirst()
                .orElse(null);
    }

    public boolean isServerAlreadyExists(String host, int port) {
        return serverList.stream()
                .anyMatch(server -> server.getHost().equals(host) && server.getPort() == port);
    }

    public boolean isServerRunning(ServerInfo server) {
        try (Socket socket = new Socket(server.getHost(), server.getPort())) {
            socket.close();
            return true;
        } catch (ConnectException e) {
            log.error("Connection refused to server: {}@{}", server.getHost(), server.getPort());
        } catch (IOException e) {
            log.error("I/O error when checking server status: {}@{}", server.getHost(), server.getPort(), e);
        }
        return false;
    }
}
