package com.example.Client.chat;

import com.example.Client.view.HomePage;
import com.example.Client.view.LoginForm;
import com.example.Client.utils.LoadBalanceManager;
import com.example.Support.TypeReceive;
import com.example.Support.DataSave;
import com.example.Support.Helper;
import com.example.Support.Send;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.JOptionPane;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.LinkedList;

public class MessageManager extends Thread {
    private static final Logger log = LogManager.getLogger(MessageManager.class);
    public LoadBalanceManager loadBalanceManager = new LoadBalanceManager();
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
            log.error("An error occurred: {} ", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String receiveMsg = this.br.readLine();
                if (receiveMsg != null) {
                    TypeReceive data = Helper.formatData(receiveMsg);
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
            log.error("An error occurred while receiving message: {}", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred while receiving message: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);

            loadBalanceManager.reconnectServerResponse();
        }
    }


    public static void sendMessage(String msg) {
        if (!msg.trim().isEmpty()) {
            HomePage.listModel.addElement("You: " + msg);
            LinkedList<String> history = DataSave.contentChat.get(DataSave.selectedUser);
            if (history == null) {
                history = new LinkedList<>();
            }
            history.add("You: " + msg);
            HomePage.tfInput.setText("");
            try {
                if (SocketManager.getSocket() != null && !SocketManager.getSocket().isClosed()) {
                    new Send(SocketManager.getSocket()).sendData("type:chat&&send:" + HomePage.myName + "&&receive:" + DataSave.selectedUser + "&&data:" + msg);
                }
            } catch (IOException e) {
                log.error("Error while sending message: {}", e.getMessage());
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
            log.error("Invalid port number format: {}", e.getMessage());
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
            log.error("Unable to connect to server: {}", e.getMessage());
        }
    }
}
