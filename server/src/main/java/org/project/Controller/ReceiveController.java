package org.project.Controller;

import src.lib.Client;
import src.lib.TypeReceive;
import src.lib.Helper;
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
import org.project.Payloads.BrokerInfo;
import org.project.Services.InterfaceMessageHandler;
import org.project.Services.ReceiveServices;
import org.project.Services.SendServices;
import org.project.Utils.CallAPI;

public class ReceiveController implements Runnable {
    private BufferedReader br;
    private Socket socket;
    public static ConcurrentHashMap<Socket, Client> receiveClientMap = new ConcurrentHashMap<>();

    public ReceiveController(Socket socket) {
        this.socket = socket;
        try {
            InputStream is = socket.getInputStream();
            this.br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            Logger.getLogger(ReceiveController.class.getName()).log(Level.SEVERE, "Cannot establish receive socket: {0}", e.getMessage());
        }
    }
    
    @Override
    public void run() {
        String receiveMsg;
        try {
            while ((receiveMsg = br.readLine()) != null) {
                TypeReceive data = Helper.FormatData(receiveMsg);
                if (data == null) {
                    continue;
                }

                InterfaceMessageHandler factory = ReceiveServices.getFactory(data.getType());
                if (factory == null) {
                    continue;
                }

                Logger.getLogger(ReceiveController.class.getName()).log(Level.INFO, "Server received: {0}", receiveMsg);
                factory.handle(data, socket, receiveMsg);
            }
        } catch (Exception e) {
            Logger.getLogger(ReceiveController.class.getName()).log(Level.SEVERE, "Error reading from socket: {0}", e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        Client currentClient;
        try {
            currentClient = receiveClientMap.get(socket);
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (currentClient != null) {
                DataSave.clients.remove(currentClient);

                CallAPI.PostData("/disconnect", currentClient.getName() + "&&localhost@" + ServerManager.PORT);
                SendServices.SendMessage(BrokerInfo.brokerSocket, "type:disconnect");
            }
        } catch (IOException e) {
            Logger.getLogger(ReceiveController.class.getName()).log(Level.SEVERE, "Error closing client socket: {0}", e.getMessage());
        }
    }
}

