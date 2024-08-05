package src.lib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;



public class SimpleHttpServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    handleClient(socket);
                } catch (IOException e) {
                    System.err.println("Client connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
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

            // Skip headers
            while (in.readLine().length() != 0) { }

            switch (method) {
                case "GET":
                    handleGetRequest(fileRequested, out, dataOut);
                    break;
                case "POST":
                    handlePostRequest(fileRequested, in, out, dataOut);
                    break;
                default:
                    sendNotImplemented(out, dataOut);
                    break;
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void handleGetRequest(String fileRequested, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        switch (fileRequested) {
            case "/json":
                sendJsonResponse(out, dataOut);
                break;
            case "/hello":
                sendHelloResponse(out, dataOut);
                break;
            case "/status":
                sendStatusResponse(out, dataOut);
                break;
            default:
                sendNotFound(out, dataOut);
                break;
        }
    }

    private static void handlePostRequest(String fileRequested, BufferedReader in, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        if (fileRequested.equals("/submit")) {
            StringBuilder requestBody = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // Skip headers
            }

            while ((line = in.readLine()) != null) {
                requestBody.append(line).append("\n");
            }

            String responseMessage = "POST data received: " + requestBody.toString();
            sendResponse(out, dataOut, "200 OK", "text/plain", responseMessage.getBytes());
        } else {
            sendNotFound(out, dataOut);
        }
    }

    private static void sendJsonResponse(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String jsonResponse = "{ \"message\": \"Hello, world!\" }";
        sendResponse(out, dataOut, "200 OK", "application/json", jsonResponse.getBytes());
    }

    private static void sendHelloResponse(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String message = "Hello from the /hello endpoint!";
        sendResponse(out, dataOut, "200 OK", "text/plain", message.getBytes());
    }

    private static void sendStatusResponse(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String statusMessage = "{ \"status\": \"Server is running\" }";
        sendResponse(out, dataOut, "200 OK", "application/json", statusMessage.getBytes());
    }

    private static void sendNotFound(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String errorMessage = "<h1>404 Not Found</h1>";
        sendResponse(out, dataOut, "404 File Not Found", "text/html", errorMessage.getBytes());
    }

    private static void sendNotImplemented(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String errorMessage = "<h1>501 Not Implemented</h1>";
        sendResponse(out, dataOut, "501 Not Implemented", "text/html", errorMessage.getBytes());
    }

    public static void sendResponse(PrintWriter out, BufferedOutputStream dataOut, String status, String contentType, byte[] content) throws IOException {
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
