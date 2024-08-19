package com.example.LoadBalance.Controller;
import com.example.LoadBalance.database.Database;
import java.io.IOException;
import java.net.Socket;

import com.example.LoadBalance.payloads.ClientInfo;
import com.example.LoadBalance.payloads.ServerInfo;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("")
public class LoadBalancerController {
    @PostMapping("/login")
    public String handleLogin(@RequestBody String requestBody) {
        // Handle login
        //Tien&&localhost@1234

        String[] nameAndServer = requestBody.split("&&");
        String[] hostAndPort = nameAndServer[1].split("@");
        
        
        String name = nameAndServer[0];
        ClientInfo client = new ClientInfo(name, nameAndServer[1]);
        Database.clients.add(client);
        
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);

        Database.serverList.forEach(server -> {
            if (server.getHost().equals(host) && server.getPort() == port) {
                server.incrementClients();
            }
        });

        return "New user login to server: " + name;

    }

    @PostMapping("/disconnect")
    public String handleDisconnect(@RequestBody String requestBody) {

        // Handle disconnect
        //Tien&&localhost@1234
        
        String[] nameAndPort = requestBody.split("&&");
        String[] hostAndPortArray = nameAndPort[1].split("@");

        String name = nameAndPort[0];
        String host = hostAndPortArray[0];
        int port = Integer.parseInt(hostAndPortArray[1]);

        Database.clients.removeIf(client -> client.getName().equals(name));
        Database.serverList.forEach(server -> {
            if (server.getHost().equals(host) && server.getPort() == port) {
                if (server.getActiveClients() > 0) {
                    server.decrementClients();
                }
            }
        });
        return "Client has disconnected from server: " + name;
        
    }

    @PostMapping("/create-group")
    public String handleCreateGroup(@RequestBody String requestBody) {
        // Handle create group
        System.out.println(requestBody);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];

        ClientInfo client = new ClientInfo(name, nameAndServer[1]);
        Database.clients.add(client);
        return "New group was created: " + name;
    }

    @PostMapping("/server-available")
    public String handleReceiveServerAvailable(@RequestBody String requestBody) {
        // Handle server available

        String[] serverAndThreadSize = requestBody.split("&&");
        String[] hostAndPort = serverAndThreadSize[0].split("@");

        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);
        int threadSize = Integer.parseInt(serverAndThreadSize[1]);

        if (Database.serverList.stream()
                .anyMatch(server -> server.getHost().equals(host) && server.getPort() == port)) {
            return "Server already exists";
        }

        Database.serverList.add(new ServerInfo(host, port, null, threadSize));

        return "New server available: " + host + "@" + port;

    }

    @PostMapping("/server-disconnected")
    public String handleServerDisconnection(@RequestBody String requestBody) {
        // Handle server disconnection

        String[] hostAndPort = requestBody.split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);


        Database.serverList.removeIf(server -> server.getHost().equals(host) && server.getPort() == port);

        return "Server has disconnected: " + host + "@" + port;
    }

    @GetMapping("/connect")
    public String handleGetConnection() {
        // Handle get connect

        ServerInfo serverEmpty = Database.serverList.stream()
                .filter(server -> isServerRunning(server) && server.getActiveClients() < server.getServerSize())
                .findFirst()
                .orElse(null);

        String responseMessage = "type:server&&data:" + serverEmpty;

        return responseMessage;
    }

    @GetMapping("/get-clients")
    public String handleGetClients() {
        // Handle get clients
        String response = "";
        for (ClientInfo client : Database.clients) {
            if (client == Database.clients.get(Database.clients.size() - 1)) {
                response += client.getName();
            } else {
                response += client.getName() + ",";
            }
        }
        System.out.println(response);
        return response;

    }


    private  boolean isServerRunning(ServerInfo server) {
        try (Socket socket = new Socket(server.getHost(), server.getPort())) {
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}