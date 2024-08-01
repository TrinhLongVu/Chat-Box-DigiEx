package broker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import src.lib.Send;
import broker.utils.Receive;;

public class MessageBroker {
    private ServerSocket brokerSocket;
    public static int PORT = 4000;
    private List<Socket> connectedServers = new ArrayList<>();

    public static void main(String[] args) {
        new MessageBroker();
    }

    public MessageBroker() {
        startMessageBroker(PORT);
    }

    public void startMessageBroker(int port) {
        try {
            brokerSocket = new ServerSocket(port);
            System.out.println("Broker listening on port " + port);

            while (true) {
                try {
                    Socket serverSocket = brokerSocket.accept();
                    connectedServers.add(serverSocket);
                    System.out.println("New server connected: " + serverSocket);

                    new Thread(new ServerHandler(serverSocket)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error accepting connection: {0}", e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
            Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error starting server: {0}", e.getMessage());
        }
    }

    private class ServerHandler implements Runnable {
        private Socket serverSocket;

        public ServerHandler(Socket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            try {
                Receive receive = new Receive(serverSocket);
                String message;
                while ((message = receive.receiveMessage()) != null) {
                    System.out.println("Received message: " + message);
                    broadcastMessage(message, serverSocket);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error reading from server: {0}", e.getMessage());
            }
        }
    }

    private void broadcastMessage(String message, Socket senderSocket) {
        for (Socket serverSocket : connectedServers) {
            if (serverSocket != senderSocket) {
                try {
                    new Send(serverSocket).sendData(message);
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error broadcasting message: {0}", e.getMessage());
                }
            }
        }
    }
}