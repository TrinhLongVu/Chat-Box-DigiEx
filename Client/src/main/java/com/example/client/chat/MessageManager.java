package com.example.client.chat;

import com.example.client.core.ClientInfo;
import com.example.client.view.HomePage;
import com.example.client.utils.LoadBalanceManager;
import com.example.support.TypeReceive;
import com.example.support.DataSave;
import com.example.support.Helper;
import com.example.support.Send;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import javax.swing.JOptionPane;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

@Component
@RequiredArgsConstructor
public class MessageManager extends Thread {
    private static final Logger log = LogManager.getLogger(MessageManager.class);
    private final FactoryClientReceive factoryClientReceive;
    private final LoadBalanceManager loadBalanceManager;
    private final SocketManager socketManager;
    private final ClientInfo clientInfo;


    public void initializeBufferedReader(Socket connSocket) {
        try {
            InputStream is = connSocket.getInputStream();
            clientInfo.setBuffer(new BufferedReader(new InputStreamReader(is)));
        } catch (IOException e) {
            log.error("An error occurred while setting buffer: {} ", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                String receiveMsg = clientInfo.getBuffer().readLine();
                if (receiveMsg != null) {
                    TypeReceive data = Helper.formatData(receiveMsg);
                    if (data.getType().equals("server")) {
                        handleServer(data.getData());
                        return;
                    }
                    MessageHandlerFactory factory = factoryClientReceive.getFactory(data.getType());
                    if (factory != null) {
                        factory.handle(data, socketManager.getSocket(), receiveMsg);
                    }
                }
            } catch (SocketException ce) {
                loadBalanceManager.reconnectServerResponse();
            } catch (Exception e) {
                log.error("An error occurred while receiving message: {}", e.getMessage());
                JOptionPane.showMessageDialog(null, "An error occurred while receiving message");
            }
        }
    }


    public void sendMessage(String msg) {
        if (!msg.trim().isEmpty()) {
            clientInfo.getMessageList().addElement("You: " + msg);
            LinkedList<String> history = DataSave.contentChat.get(DataSave.selectedUser);
            if (history == null) {
                history = new LinkedList<>();
            }
            history.add("You: " + msg);
            HomePage.tfInput.setText("");
            try {
                if (socketManager.getSocket() != null && !socketManager.getSocket().isClosed()) {
                    new Send(socketManager.getSocket()).sendData("type:chat&&send:" + clientInfo.getUserName() + "&&receive:" + DataSave.selectedUser + "&&data:" + msg);
                }
            } catch (IOException e) {
                loadBalanceManager.reconnectServerResponse();
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
            socketManager.getSocket().close();
            Socket s = new Socket(host, port);
            new Send(s).sendData("type:login&&send:" + clientInfo.getUserName());
            socketManager.setSocket(s);
            initializeBufferedReader(s);
        } catch (IOException e) {
            log.error("Unable to connect to server: {}", e.getMessage());
        }
    }
}