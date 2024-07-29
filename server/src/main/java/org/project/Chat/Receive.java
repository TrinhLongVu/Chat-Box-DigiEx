package org.project.Chat;

import src.lib.DataSave;
import src.lib.Client;
import src.lib.TypeReceive;
import src.lib.Helper;
import src.lib.Send;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

class balancer {
    public static Socket loadBalanSocket = null;
}

public class Receive implements Runnable {
    private BufferedReader br;
    private Socket socket;
    private Client currentClient;
    private static String userOnlines = "[]";
    public static ConcurrentHashMap<Socket, Client> receiveClientMap = new ConcurrentHashMap<>();

    public Receive(Socket socket) {
        this.socket = socket;
        try {
            InputStream is = socket.getInputStream();
            this.br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            throw new RuntimeException("Error initializing BufferedReader", e);
        }
    }
    

    @Override
    public void run() {
        String receiveMsg;
        try {
            while ((receiveMsg = br.readLine()) != null) {
                System.out.println("message::::" + receiveMsg);
                TypeReceive data = Helper.FormatData(receiveMsg);

                if (data == null) {
                    System.out.println("Received invalid data: " + receiveMsg);
                    continue;
                }

                if (data.getType().equals("users")) {
                    userOnlines = data.getData();
                    SendUsersOnline.handle(userOnlines);;
                    continue;
                }
                MessageHandlerFactory factory = FactoryServerReceive.getFactory(data.getType());
                if (factory != null) {
                    factory.handle(data, socket, userOnlines, receiveMsg);
                } else {
                    System.out.println("Received invalid data: " + data);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from socket: " + e.getMessage());
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Error reading from socket: {0}", e.getMessage());

        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            currentClient = receiveClientMap.get(socket);
            if (socket != null && !socket.isClosed()) {
                System.out.println("Closing connection....");
                socket.close();
            }
            if (currentClient != null) {
                DataSave.clients.remove(currentClient);
                SendUsersOnline.handle(userOnlines);
                System.out.println(
                        "Client " + currentClient.getName() + " disconnected and removed from active clients.");
                try {
                    new Send(balancer.loadBalanSocket).sendData("type:disconnect&&send:" + currentClient.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                    Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
                }

            }
        } catch (IOException e) {
            System.out.println("Error closing client socket: " + e.getMessage());
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Error closing client socket: {0}", e.getMessage());
        }
    }
}

