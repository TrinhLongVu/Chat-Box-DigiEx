package com.example.loadbalance.controllers;

import com.example.loadbalance.database.Database;
import com.example.loadbalance.payloads.ClientInfo;
import com.example.loadbalance.payloads.ServerInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/client")
public class ClientController {
    private Database database;
    private static final Logger log = LogManager.getLogger(ClientController.class);

    public ClientController(Database database) {
        this.database = database;
    }
    @PostMapping("/login")
    public ResponseEntity<String> handleLogin(@RequestBody String requestBody) {
        log.info("Received login request: {}", requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];
        String[] hostAndPort = nameAndServer[1].split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        ServerInfo server = database.getServerInfo(host, port);

        if (server == null) {
            log.warn("Server is not available: {}@{}", host, port);
            return new ResponseEntity<String>("Server is not available: " + host + "@" + port,HttpStatus.NOT_FOUND);
        }

        if (server.getActiveClients() >= server.getServerSize()) {
            log.warn("Server is full, unable to connect user: {}", name);
            return new ResponseEntity<String>("Server is full, unable to connect user: " + name,HttpStatus.NOT_FOUND);
        }

        log.info("Client connected to server: {}", name);
        database.getClients().add(new ClientInfo(name, nameAndServer[1]));
        server.incrementClients();
        return new ResponseEntity<String>("New user logged in to server: " + name,HttpStatus.OK);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<String> handleDisconnect(@RequestBody String requestBody) {
        log.info("Received disconnect request: {}", requestBody);

        String[] nameAndPort = requestBody.split("&&");
        String name = nameAndPort[0];
        String[] hostAndPortArray = nameAndPort[1].split("@");
        String host = hostAndPortArray[0];
        int port = Integer.parseInt(hostAndPortArray[1]);

        boolean clientRemoved = database.getClients().removeIf(client -> client.getName().equals(name));
        if (!clientRemoved) {
            log.warn("Client not found: {}", name);
            return new ResponseEntity<String>("Client not found: " + name,HttpStatus.NOT_FOUND);
        }

        ServerInfo server = database.getServerInfo(host, port);
        if (server != null && server.getActiveClients() > 0) {
            server.decrementClients();
            log.info("Client has disconnected from server: {}", name);
            return new ResponseEntity<String>("Client has disconnected from server: " + name,HttpStatus.OK);
        }

        log.warn("Server not found or no active clients to decrement: {}@{}", host, port);
        return new ResponseEntity<String>("Server not found or no active clients to decrement: " + host + "@" + port,HttpStatus.NOT_FOUND);
    }

    @PostMapping("/create-group")
    public ResponseEntity<String> handleCreateGroup(@RequestBody String requestBody) {
        log.info("Received create group request: {}", requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];

        database.getClients().add(new ClientInfo(name, nameAndServer[1]));
        log.info("New group was created: {}", name);
        return new ResponseEntity<String>("New group was created: " + name,HttpStatus.OK);
    }

    @PostMapping("/reconnect")
    public ResponseEntity<String> handleClientReconnection(@RequestBody String requestBody) {
        log.info("Received client reconnection request: {}", requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];
        String[] hostAndPort = nameAndServer[1].split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        database.getClients().removeIf(client -> client.getName().equals(name) && client.getServerInfo().equals(nameAndServer[1]));

        ServerInfo server = database.getServerInfo(host, port);
        if (server != null) {
            if (!database.isServerRunning(server)) {
                log.info("Old Server is not running, remove server: {}", nameAndServer[1]);
                database.getServerList().remove(server);
            } else {
                server.decrementClients();
            }
        }

        ServerInfo availableServer = database.getServerList().stream()
                .filter(s -> database.isServerRunning(s) && s.getActiveClients() < s.getServerSize())
                .findFirst()
                .orElse(null);

        String responseMessage = "type:server&&data:" + availableServer;
        log.info("Returning reconnection info: {}", responseMessage);
        return new ResponseEntity<String>(responseMessage,HttpStatus.OK);
    }
    @GetMapping("/connect")
    public ResponseEntity<String> handleGetConnection() {
        log.info("Received get connection request");

        ServerInfo availableServer = database.getServerList().stream()
                .filter(server -> database.isServerRunning(server) && server.getActiveClients() < server.getServerSize())
                .findFirst()
                .orElse(null);

        String responseMessage = availableServer != null ? "type:server&&data:" + availableServer.toString() : "No available server found";
        log.info("Returning connection info: {}", responseMessage);
        return new ResponseEntity<String>(responseMessage,HttpStatus.OK);
    }


}
