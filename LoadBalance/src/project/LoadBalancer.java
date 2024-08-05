package project;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import project.Chat.ServerInfo;
import project.Chat.Database;

import java.io.PrintWriter;
import project.Chat.ClientInfo;

public class LoadBalancer {
    private static final int MAX_CLIENTS = 2;
    private static final int PORT = 8080;
    public LoadBalancer() {
        Database.serverList = new ArrayList<>();
        Database.serverList.add(new ServerInfo("localhost", 1234, null));
        Database.serverList.add(new ServerInfo("localhost", 1235, null));
    }

    public static void main(String[] args) {
        new LoadBalancer();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    handleClient(socket);
                } catch (IOException e) {
                    Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "Cannot connect to client: {0}", e.getMessage());
                }
            }
        } catch (IOException e) {
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "Error starting LoadBalance: {0}", e.getMessage());
        }
    }
    
    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream())) {

            String inputLine = in.readLine();
            if (inputLine == null || inputLine.isEmpty()) return;

            String[] requestParts = inputLine.split(" ");
            String method = requestParts[0];
            String fileRequested = requestParts[1];

            switch (method) {
                case "POST" -> {
                    switch (fileRequested) {
                        case "/login" -> handleLogin(in, out, dataOut);
                        case "/disconnect" -> handleDisconnect(in, out, dataOut);
                        case "/create-group" -> handleCreateGroup(in, out, dataOut);
                        default -> sendNotFound(out, dataOut);
                    }
                }

                case "GET" -> {
                    switch (fileRequested) {
                        case "/connect" -> handleGetConnection(out, dataOut);
                        case "/get-clients" -> handleGetClients(out, dataOut);
                        default -> sendNotFound(out, dataOut);
                    }
                }
                default -> sendNotImplemented(out, dataOut);
            }

        } catch (IOException e) {
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "Method not supported: {0}", e.getMessage());
        }
    }
    
    
    
    private static void handleGetConnection(PrintWriter out, BufferedOutputStream dataOut) throws IOException { 
        // Find a suitable server and prepare the response
        ServerInfo serverEmpty = Database.serverList.stream()
                .filter(server -> Utils.isServerRunning(server) && server.getActiveClients() < MAX_CLIENTS)
                .findFirst()
                .orElse(null);

        String responseMessage = "type:server&&data:" + serverEmpty;
        byte[] responseData = responseMessage.getBytes();
        int responseLength = responseData.length;

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "New connection has established: {0}", responseMessage);

        // Send the response headers
        out.println("HTTP/1.1 200 OK");
        out.println("Server: SimpleJavaHttpServer");
        out.println("Content-Type: text/plain");
        out.println("Content-Length: " + responseLength);
        out.println(); // End headers with an empty line
        out.flush();

        // Send the response body
        dataOut.write(responseData);
        dataOut.flush();
    }


    private static void handleLogin(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        int contentLength = 0;
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
            }
        }

        // Read the request body based on Content-Length
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
        String requestBody = new String(body);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];
        ClientInfo client = new ClientInfo(name, nameAndServer[1]);
        Database.clients.add(client);
        String[] hostAndPort = nameAndServer[1].split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);
        Database.serverList.forEach(server -> {
            if (server.getHost().equals(host) && server.getPort() == port) {
                server.incrementClients();
            }
        });

        String responseMessage = "Receieved Message";
        byte[] responseData = responseMessage.getBytes();
        int responseLength = responseData.length;

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "New user login to server: {0}", name);

        out.println("HTTP/1.1 200 OK");
        out.println("Server: SimpleJavaHttpServer");
        out.println("Content-Type: text/plain");
        out.println("Content-Length: " + responseLength);
        out.println();
        out.flush();

        dataOut.write(responseData, 0, responseLength);
        dataOut.flush();
    }
    
    private static void handleDisconnect(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut)
            throws IOException {
        int contentLength = 0;
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
            }
        }

        // Read the request body based on Content-Length
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
        String requestBody = new String(body);
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

        String responseMessage = "Receieved Message";
        byte[] responseData = responseMessage.getBytes();
        int responseLength = responseData.length;

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Client has disconnected from server: {0}", name);

        out.println("HTTP/1.1 200 OK");
        out.println("Server: SimpleJavaHttpServer");
        out.println("Content-Type: text/plain");
        out.println("Content-Length: " + responseLength);
        out.println();
        out.flush();

        dataOut.write(responseData, 0, responseLength);
        dataOut.flush();
    }

    private static void handleCreateGroup(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        int contentLength = 0;
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
            }
        }

        // Read the request body based on Content-Length
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
        String requestBody = new String(body);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];
        ClientInfo client = new ClientInfo(name, nameAndServer[1]);
        Database.clients.add(client);

        String responseMessage = "Receieved Message";
        byte[] responseData = responseMessage.getBytes();
        int responseLength = responseData.length;

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "New group was created: {0}", name);

        out.println("HTTP/1.1 200 OK");
        out.println("Server: SimpleJavaHttpServer");
        out.println("Content-Type: text/plain");
        out.println("Content-Length: " + responseLength);
        out.println();
        out.flush();

        dataOut.write(responseData, 0, responseLength);
        dataOut.flush();
    }

    private static void handleGetClients(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String response = "";
        for (ClientInfo client : Database.clients) {
            if (client == Database.clients.get(Database.clients.size() - 1)) {
                response += client.getName();
            } else {
                response += client.getName() + ",";
            }
        }

        byte[] responseData = response.getBytes();
        int responseLength = responseData.length;

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Server-clients: {0}", response);

        out.println("HTTP/1.1 200 OK");
        out.println("Server: SimpleJavaHttpServer");
        out.println("Content-Type: text/plain");
        out.println("Content-Length: " + responseLength);
        out.println();
        out.flush();

        dataOut.write(responseData, 0, responseLength);
        dataOut.flush();
    }


    private static void sendNotFound(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: 23\r\n" +
                "\r\n" +
                "<h1>404 Not Found</h1>";

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Method not supported: {0}", errorMessage);

        out.println(errorMessage);
        out.flush();
        dataOut.write(errorMessage.getBytes());
        dataOut.flush();
    }

    private static void sendNotImplemented(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String errorMessage = "HTTP/1.1 501 Not Implemented\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: 25\r\n" +
                "\r\n" +
                "<h1>501 Not Implemented</h1>";

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Method not supported: {0}", errorMessage);

        out.println(errorMessage);
        out.flush();
        dataOut.write(errorMessage.getBytes());
        dataOut.flush();
    }
}