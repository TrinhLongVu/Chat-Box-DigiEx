package org.project.Chat;

import src.lib.Client;
import src.lib.TypeReceive;
import src.lib.Helper;
import src.lib.Send;
import src.lib.DataSave;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.project.ServerManager;
import org.project.Services.CallAPI;

class balancer {
    public static Socket loadBalanSocket = null;
}

public class Receive implements Runnable {
    private BufferedReader br;
    private Socket socket;
    private Client currentClient;
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
                System.out.println("Received message: " + receiveMsg);
                System.out.println("Step 1");

                TypeReceive data = null;
                try {
                    data = Helper.FormatData(receiveMsg);
                } catch (Exception e) {
                    System.err.println("Error formatting data: " + e.getMessage());
                }

                if (data == null) {
                    System.out.println("Data formatting returned null for message: " + receiveMsg);
                    continue; // Skip further processing if data is null
                }

                System.out.println("Step 2");

                if (data != null) {
                    System.out.println("Step 3");
                    MessageHandlerFactory factory = null;
                    try {
                        factory = FactoryServerReceive.getFactory(data.getType());
                    } catch (Exception e) {
                        System.err.println("Error getting factory: " + e.getMessage());
                    }

                    System.out.println("Step 4");

                    if (factory != null) {
                        System.out.println("Step 5");
                        try {
                            factory.handle(data, socket, receiveMsg);
                            System.out.println("Step 6");
                        } catch (Exception e) {
                            System.err.println("Error handling message: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Received invalid data: " + data);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from socket: " + e.getMessage());
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Error reading from socket: {0}", e.getMessage());
        } finally {
            System.out.println("Executing cleanup");
            cleanup();
        }        
    }

    private void cleanup() {
        System.out.println("Starting cleanup");
        try {
            currentClient = receiveClientMap.get(socket);
            if (socket != null && !socket.isClosed()) {
                System.out.println("Closing connection....");
                socket.close();
            } else {
                System.out.println("Socket is already closed or null.");
            }
            if (currentClient != null) {
                DataSave.clients.remove(currentClient);
                String dataSend = currentClient.getName() + "&&localhost@" + ServerManager.PORT;  
                CallAPI.PostData("/disconnect", dataSend);
                new Send(BrokerInfo.brokerSocket).sendData("type:disconnect");

                System.out.println(
                        "Client " + currentClient.getName() + " disconnected and removed from active clients.");
            } else {
                System.out.println("No current client associated with socket.");
            }
        } catch (IOException e) {
            System.out.println("Error closing client socket: " + e.getMessage());
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Error closing client socket: {0}", e.getMessage());
        }
    }
}
