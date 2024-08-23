package com.example.servers.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.example.servers.payloads.BrokerInfo;
import com.example.servers.services.InterfaceMessageHandler;
import com.example.servers.services.ReceiveServices;
import com.example.servers.services.SendServices;
import com.example.servers.utils.CallAPI;
import com.example.support.*;

@Component
@Scope("prototype")
public class ReceiveController implements Runnable {
    private final Logger log = LogManager.getLogger(ReceiveController.class);
    private BufferedReader br;
    private Socket socket;

    @Value("${app.server.port}")
    private int PORT;

    @Autowired
    private ReceiveServices receiveServices;

    @Autowired
    private CallAPI callAPI;

    @Autowired
    private SendServices sendServices;

    public ReceiveController(Socket socket) {
        this.socket = socket;
        try {
            InputStream is = socket.getInputStream();
            this.br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            log.error("Cannot establish receive socket: {0}", e.getMessage());
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

                InterfaceMessageHandler factory = receiveServices.getFactory(data.getType());
                if (factory != null) {
                    log.info("Server received: {0}" + receiveMsg);
                    factory.handle(data, socket, receiveMsg);
                }
            }
        } catch (Exception e) {
            log.error("Error reading from socket: {0}" + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            Client currentClient = getClientfromDataSave(socket);
            if (currentClient != null) {
                DataSave.clients.remove(currentClient);
                callAPI.PostData("/disconnect", currentClient.getName() + "&&localhost@" + PORT);
                sendServices.SendMessage(BrokerInfo.brokerSocket, "type:disconnect");
            }
        } catch (IOException e) {
            log.error("Error closing client socket: {0}" + e.getMessage());
        }
    }
    
    private Client getClientfromDataSave(Socket socket) {
        for(Client client: DataSave.clients) {
            if (client.getSocket() == socket) {
                return client;
            }
        }
        return null;
    }
}

