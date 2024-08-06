package project;

import java.io.PrintWriter;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import project.Chat.ClientInfo;
import project.Chat.Database;
import project.Chat.ServerInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

import src.lib.Helper;
import src.lib.TypeReceive;

public class LoadBalancer {
    // MAX_CLIENTS is the maximum number of clients that can connect to a server (MUST BE LARGER 1 CLIENTS THAN REAL SERVER)
    private static final int MAX_CLIENTS = 2;
    private static final int PORT = 8080;


    public static void main(String[] args) {
        Database.serverList = new ArrayList<>();
        Database.serverList.add(new ServerInfo("localhost", 1235, null));
        Database.serverList.add(new ServerInfo("localhost", 1234, null));
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

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "New connection has established: {0}", responseMessage);

        TypeReceive data = Helper.FormatData(responseMessage);
        if (data.getData() == null) {
            sendResponse(out, dataOut, "200", responseMessage, null);
        } else {
            sendResponse(out, dataOut, "200", responseMessage, responseData);
        }
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

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "New user login to server: {0}", name);

        sendResponse(out, dataOut, "200", responseMessage, responseData);
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

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Client has disconnected from server: {0}", name);

        sendResponse(out, dataOut, "200", responseMessage, responseData);
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

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "New group was created: {0}", name);

        sendResponse(out, dataOut, "200", responseMessage, responseData);
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


        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Server-clients: {0}", response);

        sendResponse(out, dataOut, "200", response, responseData);
    }


    private static void sendNotFound(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String errorMessage = """
                              HTTP/1.1 404 File Not Found\r
                              Content-Type: text/html\r
                              Content-Length: 23\r
                              \r
                              <h1>404 Not Found</h1>""";

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Method not supported: {0}", errorMessage);

        out.println(errorMessage);
        out.flush();
        dataOut.write(errorMessage.getBytes());
        dataOut.flush();
    }

    private static void sendNotImplemented(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String errorMessage = """
                              HTTP/1.1 501 Not Implemented\r
                              Content-Type: text/html\r
                              Content-Length: 25\r
                              \r
                              <h1>501 Not Implemented</h1>""";

        Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Method not supported: {0}", errorMessage);

        out.println(errorMessage);
        out.flush();
        dataOut.write(errorMessage.getBytes());
        dataOut.flush();
    }

    private static void sendResponse(PrintWriter out, BufferedOutputStream dataOut, String status, String contentType, byte[] content) throws IOException {
        out.println("HTTP/1.1 " + status);
        out.println("Server: SimpleJavaHttpServer");
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + content.length);
        out.println();
        out.flush();

        dataOut.write(content, 0, content.length);
        dataOut.flush();
    }
}