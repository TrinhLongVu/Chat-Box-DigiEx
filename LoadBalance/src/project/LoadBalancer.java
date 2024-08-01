package project;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import src.lib.Helper;
import src.lib.Send;
import src.lib.TypeReceive;

import org.project.ServerManager;

import project.Chat.ServerInfo;
import project.Chat.ServerManagerInfo;
import project.Chat.Database;
import project.Chat.Receive;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import project.Chat.ClientInfo;

public class LoadBalancer {
    private static final int MAX_CLIENTS = 2;

    public LoadBalancer() {
        Database.serverList = new ArrayList<>();
        Database.serverList.add(new ServerInfo("localhost", 1234, null));
        Database.serverList.add(new ServerInfo("localhost", 1235, null));
    }

    public static void main(String[] args) {
        LoadBalancer loadBalancer = new LoadBalancer();
        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            // Define a context that listens for requests
            server.createContext("/", new MyHandler());
            server.createContext("/login", new HandlerLogin());
            server.createContext("/get-clients", new HandlerGetClients());

            // Start the server
            server.setExecutor(null);
            server.start();
            System.out.println("LoadBalancer is running on http://localhost:8080");
        } catch (IOException e) {
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
        }
    }
    
    // Handler that processes incoming requests
    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Read the request from the client
            ServerInfo serverEmpty = Database.serverList.stream()
                    .filter(server -> server.getActiveClients() < MAX_CLIENTS)
                    .findFirst()
                    .orElse(null);

            InputStream is = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client: " + line);
            }
            reader.close(); // Close the input stream after reading
            String response = "type:server&&data:" + serverEmpty.toString();



            // Set the response headers and status code
            exchange.sendResponseHeaders(200, response.length());

            try ( // Write the response body
                    OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            } catch (IOException e) {
                Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}",
                        e.getMessage());
            }
        }
    }
    
    static class HandlerLogin implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Received login request");
            InputStream is = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received from client: " + line);
                String[] nameAndServer = line.split("&&");
                String name = nameAndServer[0];
                ClientInfo client = new ClientInfo(name, nameAndServer[1]);
                Database.clients.add(client);
                String[] hostAndPort = nameAndServer[1].split("@");
                String host = hostAndPort[0];
                int port = Integer.parseInt(hostAndPort[1]);
                Database.serverList.forEach(server -> {
                    if (server.getHost().equals(host) && server.getPort() == port) {
                        server.incrementClients();
                        System.out.println("Incremented clients for server: " + server.toString());
                        System.out.println("Number: " + server.getActiveClients());
                    }
                });

            }

            reader.close(); // Close the input stream after reading
            String response = "Receieved Message";

            // Set the response headers and status code
            exchange.sendResponseHeaders(200, response.length());

            try ( // Write the response body
                    OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            } catch (IOException e) {
                Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}",
                        e.getMessage());
            }

        }
    }
    static class HandlerGetClients implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Received get-clients request");
            String response = "";
            for (ClientInfo client : Database.clients) {
                if (client == Database.clients.get(Database.clients.size() - 1)) {
                    response += client.getName();
                }
                else {
                    response += client.getName() + ", ";
                }
            }

            // Set the response headers and status code
            exchange.sendResponseHeaders(200, response.length());

            try ( // Write the response body
                    OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            } catch (IOException e) {
                Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}",
                        e.getMessage());
            }
        }
    }
}

