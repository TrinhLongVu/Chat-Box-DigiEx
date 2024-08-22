package com.example.client.chat;

import com.example.client.core.Message;
import com.example.client.view.HomePage;
import com.example.client.view.LoginForm;
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
import java.util.LinkedList;

@Component
@RequiredArgsConstructor
public class MessageManager extends Thread {
    private static final Logger log = LogManager.getLogger(MessageManager.class);
    private final LoadBalanceManager loadBalanceManager;
    private final SocketManager socketManager;
    private final LoginForm loginForm;
    private final HomePage homePage;
    private final Message message;


    private void initializeBufferedReader(Socket connSocket) {
        try {
            InputStream is = connSocket.getInputStream();
            message.setBuffer(new BufferedReader(new InputStreamReader(is)));
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
                String receiveMsg = message.getBuffer().readLine();
                if (receiveMsg != null) {
                    TypeReceive data = Helper.formatData(receiveMsg);
                    if (data.getType().equals("server")) {
                        handleServer(data.getData());
                        return;
                    }
                    MessageHandlerFactory factory = FactoryClientReceive.getFactory(data.getType());
                    if (factory != null) {
                        factory.handle(data, socketManager.getSocket(), receiveMsg);
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


    public void sendMessage(String msg) {
        if (!msg.trim().isEmpty()) {
            HomePage.listModel.addElement("You: " + msg);
            LinkedList<String> history = DataSave.contentChat.get(DataSave.selectedUser);
            if (history == null) {
                history = new LinkedList<>();
            }
            history.add("You: " + msg);
            HomePage.tfInput.setText("");
            try {
                if (socketManager.getSocket() != null && !socketManager.getSocket().isClosed()) {
                    new Send(socketManager.getSocket()).sendData("type:chat&&send:" + homePage.getName() + "&&receive:" + DataSave.selectedUser + "&&data:" + msg);
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
            socketManager.getSocket().close();
            Socket s = new Socket(host, port);
            new Send(s).sendData("type:login&&send:" + LoginForm.userName);
            socketManager.setSocket(s);
            String userName = loginForm.getName();
            homePage.setName(userName);
            homePage.init();
            initializeBufferedReader(s);
        } catch (IOException e) {
            log.error("Unable to connect to server: {}", e.getMessage());
        }
    }
}
