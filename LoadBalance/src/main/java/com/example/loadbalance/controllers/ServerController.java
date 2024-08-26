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
@RequestMapping("/server")
public class ServerController {
    private Database database;
    private static final Logger log = LogManager.getLogger(ServerController.class);

    public ServerController(Database database) {
        this.database = database;
    }
    @PostMapping("/server-available")
    public ResponseEntity<String> handleReceiveServerAvailable(@RequestBody String requestBody) {
        log.info("Received server available request: {}", requestBody);

        String[] serverAndThreadSize = requestBody.split("&&");
        String[] hostAndPort = serverAndThreadSize[0].split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);
        int threadSize = Integer.parseInt(serverAndThreadSize[1]);

        if (database.isServerAlreadyExists(host, port)) {
            log.warn("Server already exists: {}@{}", host, port);
            return new ResponseEntity<String>("Server already exists", HttpStatus.BAD_REQUEST);
        }

        if (!database.isServerRunning(new ServerInfo(host, port, null, threadSize))) {
            log.warn("Server is not running: {}@{}", host, port);
            return new ResponseEntity<String>("Server is not running", HttpStatus.BAD_REQUEST);
        }

        database.getServerList().add(new ServerInfo(host, port, null, threadSize));
        log.info("New server available: {}@{}", host, port);
        return new ResponseEntity<String>("New server available: " + host + "@" + port, HttpStatus.OK);
    }

    @PostMapping("/server-disconnected")
    public ResponseEntity<String> handleServerDisconnection(@RequestBody String requestBody) {
        log.info("Received server disconnection request: {}", requestBody);

        String[] hostAndPort = requestBody.split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        boolean removedServer = database.getServerList().removeIf(server -> server.getHost().equals(host) && server.getPort() == port);
        if (!removedServer) {
            log.warn("Server not found: {}@{}", host, port);
            return new ResponseEntity<String>("Server not found: " + host + "@" + port, HttpStatus.NOT_FOUND);
        }

        log.info("Server has disconnected: {}@{}", host, port);
        return new ResponseEntity<String>("Server has disconnected: " + host + "@" + port, HttpStatus.OK);
    }

    @GetMapping("/get-clients")
    public ResponseEntity<String> handleGetClients() {
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
        return new ResponseEntity<String>(response.toString(), HttpStatus.OK);
    }


}
