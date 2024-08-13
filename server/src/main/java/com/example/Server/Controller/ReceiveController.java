package com.example.Server.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.Server.ServerManager;
import com.example.Server.payloads.BrokerInfo;
import com.example.Server.services.InterfaceMessageHandler;
import com.example.Server.services.ReceiveServices;
import com.example.Server.services.SendServices;
import com.example.Server.utils.CallAPI;
import com.example.Support.lib.Client;
import com.example.Support.lib.DataSave;
import com.example.Support.lib.Helper;
import com.example.Support.lib.TypeReceive;

public class ReceiveController implements Runnable {
    private BufferedReader br;
    private Socket socket;
    public static Map<Socket, Client> receiveClientMap = new HashMap();

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
                TypeReceive data = Helper.formatData(receiveMsg);
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

