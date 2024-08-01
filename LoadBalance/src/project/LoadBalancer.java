package project;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import src.lib.Send;
import org.project.ServerManager;

import project.Chat.ServerInfo;
import project.Chat.ServerManagerInfo;
import project.Chat.Database;
import project.Chat.Receive;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class LoadBalancer {
    private static int LOAD_BALANCER_PORT;
    private static final int MAX_CLIENTS = 2;
    private static final int INITIAL_PORT = 1234;
    private int portDefault = INITIAL_PORT;

    public LoadBalancer() {
        Database.serverList = new ArrayList<>();
        Database.serverList.add(new ServerInfo("localhost", 1234, null));
        Database.serverList.add(new ServerInfo("localhost", 1235, null));

        


        // LOAD_BALANCER_PORT = port;
        // Database.serverList = new ArrayList<>();

        // try {
        //     initializeServerManager();
        // } catch (IOException e) {
        //     Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());

        // }
    }

    public static void main(String[] args) {
        LoadBalancer loadBalancer = new LoadBalancer();
        try{
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            // Define a context that listens for requests
            server.createContext("/", new MyHandler());

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
            String response = "type:server&&data:" + serverEmpty.toString();
            

            // Set the response headers and status code
            exchange.sendResponseHeaders(200, response.length());

            try ( // Write the response body
                    OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            } catch (IOException e) {
                Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
            }
        }
    }
}

