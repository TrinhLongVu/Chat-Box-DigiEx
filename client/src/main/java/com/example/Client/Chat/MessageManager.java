package com.example.Client.chat;

import com.example.Client.View.HomePage;
import com.example.Client.View.LoginForm;
import src.lib.TypeReceive;
import com.example.Client.utils.LoadBalanceManager;

import javax.swing.JOptionPane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.LinkedList;

import src.lib.DataSave;
import src.lib.Helper;
import src.lib.Send;

public class MessageManager extends Thread {
    public LoadBalanceManager loadBalanceManager = new LoadBalanceManager();
    public static boolean isClose = false;
    private String receiveMsg = "";
    private BufferedReader br;

    public MessageManager(Socket connSocket) {
        SocketManager.setSocket(connSocket);
        initializeBufferedReader(connSocket);
    }

    private void initializeBufferedReader(Socket connSocket) {
        try {
            InputStream is = connSocket.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, "An error occurred: {0} ", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                this.receiveMsg = this.br.readLine();
                if (receiveMsg != null) {
                    TypeReceive data = Helper.FormatData(receiveMsg);
                    if (data.getType().equals("server")) {
                        handleServer(data.getData());
                        return;
                    }
                    MessageHandlerFactory factory = FactoryClientReceive.getFactory(data.getType());
                    if (factory != null) {
                        factory.handle(data, SocketManager.getSocket(), receiveMsg);
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE,
                    "An error occurred while receiving message: {0}", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred while receiving message: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);

            loadBalanceManager.reconnectServerResponse();
        }
    }


    public static void sendMessage(String msg) {
        String message = msg;
        if (!message.trim().isEmpty()) {
            HomePage.listModel.addElement("You: " + message);
            LinkedList<String> history = DataSave.contentChat.get(DataSave.selectedUser);
            if (history == null) {
                history = new LinkedList<>();
            }
            history.add("You: " + message);
            HomePage.tfInput.setText("");
            try {
                if (SocketManager.getSocket() != null && !SocketManager.getSocket().isClosed()) {
                    new Send(SocketManager.getSocket()).sendData("type:chat&&send:" + HomePage.myName + "&&receive:" + DataSave.selectedUser + "&&data:" + message);
                }
            } catch (IOException e) {
                Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, "Error while sending message: {0}",
                    e.getMessage());
            }
        }
    }
    

    private void handleServer(String data) {
        String[] hostAndPort = data.split("@");
        int port;
        String host = hostAndPort[0];

        try {
            port = Integer.parseInt(hostAndPort[1]);
        } catch (NumberFormatException e) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.SEVERE, "Invalid port number format: {0}",
                    e.getMessage());
            return;
        }

        try {
            SocketManager.getSocket().close();
            Socket s = new Socket(host, port);
            new Send(s).sendData("type:login&&send:" + LoginForm.userName);
            new HomePage(null, LoginForm.userName);
            SocketManager.setSocket(s);
            initializeBufferedReader(s); // Reinitialize BufferedReader with new socket
        } catch (IOException e) {
            Logger.getLogger(MessageManager.class.getName()).log(Level.WARNING, "Unable to connect to server: {0}",
                    e.getMessage());
        }
    }
}
