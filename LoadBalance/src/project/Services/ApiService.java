package project.Services;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import project.Utils;
import project.Database.Database;
import project.Payloads.ClientInfo;
import project.Payloads.ServerInfo;
import src.lib.Helper;
import src.lib.TypeReceive;

public class ApiService {

    public static void handleGetConnection(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        // Find a suitable server and prepare the response
        ServerInfo serverEmpty = Database.serverList.stream()
                .filter(server -> Utils.isServerRunning(server) && server.getActiveClients() < server.getServerSize())
                .findFirst()
                .orElse(null);

        String responseMessage = "type:server&&data:" + serverEmpty;


        sendResponse(out, dataOut, "200", responseMessage, "New connection has established: " + responseMessage);
    }
    
    public static void handleServerDisconnection(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String requestBody = getRequestBody(in);

        String[] hostAndPort = requestBody.split("@");
        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);


        Database.serverList.removeIf(server -> server.getHost().equals(host) && server.getPort() == port);


        sendResponse(out, dataOut, "200", "Received Message", "Server has disconnected: " + host + "@" + port);
    }



    public static void handleReceiveServerAvailable(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String requestBody = getRequestBody(in);

        String[] serverAndThreadSize = requestBody.split("&&");
        String[] hostAndPort = serverAndThreadSize[0].split("@");

        String host = hostAndPort[0];
        int port = Integer.parseInt(hostAndPort[1]);
        int threadSize = Integer.parseInt(serverAndThreadSize[1]);

        if (Database.serverList.stream()
                .anyMatch(server -> server.getHost().equals(host) && server.getPort() == port)) {
            sendResponse(out, dataOut, "400", "Server already exists", "Server already exists");
            return;
        }

        Database.serverList.add(new ServerInfo(host, port, null, threadSize));


        sendResponse(out, dataOut, "200", "Received Message", "New server available: " + host + "@" + port);
    }

    public static void handleLogin(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String requestBody = getRequestBody(in);

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

        sendResponse(out, dataOut, "200", "Received Message", "New user login to server: " + name);
    }
    
    public static void handleDisconnect(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut)
            throws IOException {
        String requestBody = getRequestBody(in);
        
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
        
        sendResponse(out, dataOut, "200", "Received Message", "Client has disconnected from server: " + name);
    }

    public static void handleCreateGroup(BufferedReader in, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String requestBody = getRequestBody(in);

        String[] nameAndServer = requestBody.split("&&");
        String name = nameAndServer[0];

        ClientInfo client = new ClientInfo(name, nameAndServer[1]);
        Database.clients.add(client);

        sendResponse(out, dataOut, "200", "Received Message", "New group was created: " + name);
    }

    public static void handleGetClients(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String response = "";
        for (ClientInfo client : Database.clients) {
            if (client == Database.clients.get(Database.clients.size() - 1)) {
                response += client.getName();
            } else {
                response += client.getName() + ",";
            }
        }

        sendResponse(out, dataOut, "200", response, "Get all clients");
    }
    
    private static String getRequestBody(BufferedReader in) throws IOException {
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
        return requestBody;
    }

    private static void sendResponse(PrintWriter out, BufferedOutputStream dataOut, String status, String contentType,
            String notification) throws IOException {
        
        byte[] content = contentType.getBytes();
        
        if (status.equals("200")) {
            Logger.getLogger(ApiService.class.getName()).log(Level.INFO, notification);
        } else {
            Logger.getLogger(ApiService.class.getName()).log(Level.WARNING, notification);
        }

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
