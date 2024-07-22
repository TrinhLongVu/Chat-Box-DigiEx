package org.project;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.project.Chat.Receive;

public class ServerManager {
    private ServerSocket serverSocket;
    private boolean running;

    public void startServer(int port) {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                while (running) {
                    Socket socket = serverSocket.accept();
                    new Receive(socket).start();
                }
            } catch (IOException e) {
                System.out.println("Error starting server: " + e.getMessage());
            }
        }).start();
    }

    public void stopServer() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
                System.out.println("Server stopped.");
            } catch (IOException e) {
                System.out.println("Error stopping server: " + e.getMessage());
            }
        }
    }

    public boolean isRunning() {
        return running;
    }
}