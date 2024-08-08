package project;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import project.Database.Database;
import project.Services.ApiService;

public class LoadBalancer {
    private static final int PORT = 8080;
    private static final  Map<String, Socket> activeConnections = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Database.serverList = new ArrayList<>();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    String clientAddress = socket.getRemoteSocketAddress().toString();
                    activeConnections.put(clientAddress, socket);
                    handleClient(socket);
                    disconnectClient(clientAddress);
                } catch (IOException e) {
                    Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "Cannot connect to client: {0}", e.getMessage());
                }
            }
        } catch (IOException e) {
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "Error starting LoadBalance: {0}",
                    e.getMessage());
        }
    }
    
    private static void disconnectClient(String clientAddress) {
        Socket socket = activeConnections.get(clientAddress);
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                Logger.getLogger(LoadBalancer.class.getName()).log(Level.INFO, "Disconnected client: {0}",
                        clientAddress);
                activeConnections.remove(clientAddress);
            } catch (IOException e) {
                Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "Error disconnecting client: {0}",
                        e.getMessage());
            }
        }
    }

    
    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            BufferedOutputStream dataOut = new BufferedOutputStream(socket.getOutputStream())) {

            String inputLine = in.readLine();
            if (inputLine == null || inputLine.isEmpty()) {
                return;
            }

            String[] requestParts = inputLine.split(" ");
            String method = requestParts[0];
            String fileRequested = requestParts[1];

            switch (method) {
                case "POST" -> {
                    switch (fileRequested) {
                        case "/login" -> ApiService.handleLogin(in, out, dataOut);
                        case "/disconnect" -> ApiService.handleDisconnect(in, out, dataOut);
                        case "/create-group" -> ApiService.handleCreateGroup(in, out, dataOut);
                        case "/server-available" -> ApiService.handleReceiveServerAvailable(in, out, dataOut);
                        case "/server-disconnected" -> ApiService.handleServerDisconnection(in, out, dataOut);
                        default -> sendNotFound(out, dataOut);
                    }
                }

                case "GET" -> {
                    switch (fileRequested) {
                        case "/connect" -> ApiService.handleGetConnection(out, dataOut);
                        case "/get-clients" -> ApiService.handleGetClients(out, dataOut);
                        default -> sendNotFound(out, dataOut);
                    }
                }
                default -> sendNotImplemented(out, dataOut);
            }

        } catch (IOException e) {
            Logger.getLogger(LoadBalancer.class.getName()).log(Level.SEVERE, "Method not supported: {0}", e.getMessage());
        }
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

    
}