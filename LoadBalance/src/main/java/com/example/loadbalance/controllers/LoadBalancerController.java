package com.example.loadbalance.controllers;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.loadbalance.database.Database;
import com.example.loadbalance.payloads.ClientInfo;
import com.example.loadbalance.payloads.ServerInfo;

import javax.xml.crypto.Data;

@RestController
@RequestMapping("")
public class LoadBalancerController {
    private static final Logger log = LogManager.getLogger(LoadBalancerController.class);

    @PostMapping("/login")
    public String handleLogin(@RequestBody String requestBody) {
        log.info("Received login request: {}", requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String[] hostAndPort = nameAndServer[1].split("@");

        String name = nameAndServer[0];
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        for (ServerInfo server : Database.serverList) {
            if (server.getHost().equals(host) && server.getPort() == port && isServerRunning(server)) {
                if (server.getActiveClients() < server.getServerSize()) {
                    log.info("Client connected to server: {}", name);
                    ClientInfo client = new ClientInfo(name, nameAndServer[1]);
                    Database.clients.add(client);
                    server.incrementClients();
                    return "New user logged in to server: " + name;
                } else {
                    log.warn("Server is full, unable to connect user: {}", name);
                    return "Server is full, unable to connect user: " + name;
                }
            }
        }

        log.warn("Server is not available: {}@{}", host, port);
        return "Server is not available: " + host + "@" + port;
    }

    @PostMapping("/disconnect")
    public String handleDisconnect(@RequestBody String requestBody) {
        log.info("Received disconnect request: {}", requestBody);

        String[] nameAndPort = requestBody.split("&&");
        String[] hostAndPortArray = nameAndPort[1].split("@");

        String name = nameAndPort[0];
        String host = hostAndPortArray[0];
        int port = Integer.parseInt(hostAndPortArray[1]);

        boolean clientRemoved = Database.clients.removeIf(client -> client.getName().equals(name));

        if (!clientRemoved) {
            log.warn("Client not found: {}", name);
            return "Client not found: " + name;
        }

        boolean serverUpdated = false;
        for (ServerInfo server : Database.serverList) {
            if (server.getHost().equals(host) && server.getPort() == port && isServerRunning(server)) {
                if (server.getActiveClients() > 0) {
                    server.decrementClients();
                    serverUpdated = true;
                }
                break;
            }
        }

        if (!serverUpdated) {
            log.warn("Server not found or no active clients to decrement: {}@{}", host, port);
            return "Server not found or no active clients to decrement: " + host + "@" + port;
        }

        log.info("Client has disconnected from server: {}", name);
        return "Client has disconnected from server: " + name;
    }

    @PostMapping("/create-group")
    public String handleCreateGroup(@RequestBody String requestBody) {
        log.info("Received create group request: {}", requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];

        ClientInfo client = new ClientInfo(name, nameAndServer[1]);
        Database.clients.add(client);
        log.info("New group was created: {}", name);
        return "New group was created: " + name;
    }

    @PostMapping("/server-available")
    public String handleReceiveServerAvailable(@RequestBody String requestBody) {
        log.info("Received server available request: {}", requestBody);

        String[] serverAndThreadSize = requestBody.split("&&");
        String[] hostAndPort = serverAndThreadSize[0].split("@");

        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);
        int threadSize = Integer.parseInt(serverAndThreadSize[1]);

        if (Database.serverList.stream()
                .anyMatch(server -> server.getHost().equals(host) && server.getPort() == port)) {
            log.warn("Server already exists: {}@{}", host, port);
            return "Server already exists";
        }
        if (!isServerRunning(new ServerInfo(host, port, null, threadSize))) {
            log.warn("Server is not running: {}@{}", host, port);
            return "Server is not running";
        }
        Database.serverList.add(new ServerInfo(host, port, null, threadSize));
        log.info("New server available: {}@{}", host, port);
        return "New server available: " + host + "@" + port;
    }

    @PostMapping("/server-disconnected")
    public String handleServerDisconnection(@RequestBody String requestBody) {
        log.info("Received server disconnection request: {}", requestBody);

        String[] hostAndPort = requestBody.split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        boolean removedServer = Database.serverList.removeIf(server -> server.getHost().equals(host) && server.getPort() == port);

        if (!removedServer) {
            log.warn("Server not found: {}@{}", host, port);
            return "Server not found: " + host + "@" + port;
        }

        log.info("Server has disconnected: {}@{}", host, port);
        return "Server has disconnected: " + host + "@" + port;
    }

    @PostMapping("/reconnect")
    public String handleClientReconnection(@RequestBody String requestBody){
        log.info("Received client reconnection request: {}", requestBody);
        //clientName&&localhost@1234
        String[] nameAndServer = requestBody.split("&&");
        String[] hostAndPort = nameAndServer[1].split("@");
        String name = nameAndServer[0];
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        Database.clients.forEach(client -> log.info("Before Client: {}", client));
        Database.clients.removeIf(client -> client.getName().equals(name) && client.getServerInfo().equals(nameAndServer[1]));

        for (ServerInfo server : Database.serverList) {
            if (server.getHost().equals(host) && server.getPort() == port) {
                if(!isServerRunning(server)) {
                    log.info("Old Server is not running, remove server: {}", nameAndServer[1]);
                    Database.serverList.remove(server);
                }else {
                    server.decrementClients();
                }
                break;
            }
        }
        Database.serverList.forEach(server -> log.info("Server: {}", server));
        Database.clients.forEach(client -> log.info("After Client: {}", client));

        ServerInfo serverEmpty = Database.serverList.stream()
                .filter(server -> isServerRunning(server) && server.getActiveClients() < server.getServerSize()
                        && (!server.getHost().equals(host) || server.getPort() != port))
                .findFirst()
                .orElse(null);

        String responseMessage = "type:server&&data:" + serverEmpty;
        log.info("Returning reconnection info: {}", responseMessage);
        return responseMessage;
    }
    @GetMapping("/connect")
    public String handleGetConnection() {
        log.info("Received get connection request");

        ServerInfo serverEmpty = Database.serverList.stream()
                .filter(server -> isServerRunning(server) && server.getActiveClients() < server.getServerSize())
                .findFirst()
                .orElse(null);

        String responseMessage = "type:server&&data:" + serverEmpty;
        log.info("Returning connection info: {}", responseMessage);
        return responseMessage;
    }

    @GetMapping("/get-clients")
    public String handleGetClients() {
        log.info("Received get clients request");

        StringBuilder response = new StringBuilder();
        for (ClientInfo client : Database.clients) {
            if (client == Database.clients.get(Database.clients.size() - 1)) {
                response.append(client.getName());
            } else {
                response.append(client.getName()).append(",");
            }
        }
        log.info("Returning clients info: {}", response);
        return response.toString();
    }

    private boolean isServerRunning(ServerInfo server) {
        try (Socket socket = new Socket(server.getHost(), server.getPort())) {
            socket.close();
            return true;
        }
        catch (ConnectException e){
            log.error("Server is not running: {}@{}", server.getHost(), server.getPort(), e);
            return false;
        }
        catch (IOException e) {
            log.error("Server is not running: {}@{}", server.getHost(), server.getPort(), e);
            return false;
        }
    }
}