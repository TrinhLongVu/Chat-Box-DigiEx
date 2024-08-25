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



@RestController
@RequestMapping("")
public class LoadBalancerController {
    private Database database;
    private static final Logger log = LogManager.getLogger(LoadBalancerController.class);

    public LoadBalancerController(Database database) {
        this.database = database;
    }
    @PostMapping("/login")
    public String handleLogin(@RequestBody String requestBody) {
        log.info("Received login request: {}", requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];
        String[] hostAndPort = nameAndServer[1].split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        ServerInfo server = getServerInfo(host, port);

        if (server == null) {
            log.warn("Server is not available: {}@{}", host, port);
            return "Server is not available: " + host + "@" + port;
        }

        if (server.getActiveClients() >= server.getServerSize()) {
            log.warn("Server is full, unable to connect user: {}", name);
            return "Server is full, unable to connect user: " + name;
        }

        log.info("Client connected to server: {}", name);
        database.getClients().add(new ClientInfo(name, nameAndServer[1]));
        server.incrementClients();
        return "New user logged in to server: " + name;
    }

    @PostMapping("/disconnect")
    public String handleDisconnect(@RequestBody String requestBody) {
        log.info("Received disconnect request: {}", requestBody);

        String[] nameAndPort = requestBody.split("&&");
        String name = nameAndPort[0];
        String[] hostAndPortArray = nameAndPort[1].split("@");
        String host = hostAndPortArray[0];
        int port = Integer.parseInt(hostAndPortArray[1]);

        boolean clientRemoved = database.getClients().removeIf(client -> client.getName().equals(name));
        if (!clientRemoved) {
            log.warn("Client not found: {}", name);
            return "Client not found: " + name;
        }

        ServerInfo server = getServerInfo(host, port);
        if (server != null && server.getActiveClients() > 0) {
            server.decrementClients();
            log.info("Client has disconnected from server: {}", name);
            return "Client has disconnected from server: " + name;
        }

        log.warn("Server not found or no active clients to decrement: {}@{}", host, port);
        return "Server not found or no active clients to decrement: " + host + "@" + port;
    }

    @PostMapping("/create-group")
    public String handleCreateGroup(@RequestBody String requestBody) {
        log.info("Received create group request: {}", requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];

        database.getClients().add(new ClientInfo(name, nameAndServer[1]));
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

        if (isServerAlreadyExists(host, port)) {
            log.warn("Server already exists: {}@{}", host, port);
            return "Server already exists";
        }

        if (!isServerRunning(new ServerInfo(host, port, null, threadSize))) {
            log.warn("Server is not running: {}@{}", host, port);
            return "Server is not running";
        }

        database.getServerList().add(new ServerInfo(host, port, null, threadSize));
        log.info("New server available: {}@{}", host, port);
        return "New server available: " + host + "@" + port;
    }

    @PostMapping("/server-disconnected")
    public String handleServerDisconnection(@RequestBody String requestBody) {
        log.info("Received server disconnection request: {}", requestBody);

        String[] hostAndPort = requestBody.split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        boolean removedServer = database.getServerList().removeIf(server -> server.getHost().equals(host) && server.getPort() == port);
        if (!removedServer) {
            log.warn("Server not found: {}@{}", host, port);
            return "Server not found: " + host + "@" + port;
        }

        log.info("Server has disconnected: {}@{}", host, port);
        return "Server has disconnected: " + host + "@" + port;
    }

    @PostMapping("/reconnect")
    public String handleClientReconnection(@RequestBody String requestBody) {
        log.info("Received client reconnection request: {}", requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];
        String[] hostAndPort = nameAndServer[1].split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        database.getClients().removeIf(client -> client.getName().equals(name) && client.getServerInfo().equals(nameAndServer[1]));

        ServerInfo server = getServerInfo(host, port);
        if (server != null) {
            if (!isServerRunning(server)) {
                log.info("Old Server is not running, remove server: {}", nameAndServer[1]);
                database.getServerList().remove(server);
            } else {
                server.decrementClients();
            }
        }

        ServerInfo availableServer = database.getServerList().stream()
                .filter(s -> isServerRunning(s) && s.getActiveClients() < s.getServerSize())
                .findFirst()
                .orElse(null);

        String responseMessage = "type:server&&data:" + availableServer;
        log.info("Returning reconnection info: {}", responseMessage);
        return responseMessage;
    }
    @GetMapping("/connect")
    public String handleGetConnection() {
        log.info("Received get connection request");

        ServerInfo availableServer = database.getServerList().stream()
                .filter(server -> isServerRunning(server) && server.getActiveClients() < server.getServerSize())
                .findFirst()
                .orElse(null);

        String responseMessage = availableServer != null ? availableServer.toString() : "No available server found";
        log.info("Returning connection info: {}", responseMessage);
        return responseMessage;
    }

    @GetMapping("/get-clients")
    public String handleGetClients() {
        log.info("Received get clients request");

        StringBuilder response = new StringBuilder();
        for (ClientInfo client : database.getClients()) {
            if (client == database.getClients().get(database.getClients().size() - 1)) {
                response.append(client.getName());
            } else {
                response.append(client.getName()).append(",");
            }
        }
        log.info("Returning clients info: {}", response);
        return response.toString();
    }

    private ServerInfo getServerInfo(String host, int port) {
        return database.getServerList().stream()
                .filter(server -> server.getHost().equals(host) && server.getPort() == port)
                .findFirst()
                .orElse(null);
    }

    private boolean isServerAlreadyExists(String host, int port) {
        return database.getServerList().stream()
                .anyMatch(server -> server.getHost().equals(host) && server.getPort() == port);
    }

    private boolean isServerRunning(ServerInfo server) {
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