package com.example.client.chat;

import java.net.Socket;
import java.io.IOException;
import java.util.LinkedList;
import com.example.support.Send;
import com.example.support.Helper;
import com.example.support.DataSave;
import lombok.RequiredArgsConstructor;
import com.example.support.TypeReceive;
import org.apache.logging.log4j.Logger;
import com.example.client.view.HomePage;
import com.example.client.core.ClientInfo;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;
import com.example.client.utils.LoadBalanceManager;
import org.springframework.scheduling.annotation.Scheduled;

@Component
@RequiredArgsConstructor
public class MessageManager {
    private static final Logger log = LogManager.getLogger(MessageManager.class);
    private final FactoryClientReceive factoryClientReceive;
    private final LoadBalanceManager loadBalanceManager;
    private final SocketManager socketManager;
    private final ClientInfo clientInfo;

    @Scheduled(fixedRate = 500)
    public void receiveServerMessage() {
        try {
            String receiveMsg ;
            log.info(socketManager.getBuffer().toString());
            while ((receiveMsg = socketManager.getBuffer().readLine()) != null) {
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
        } catch (IOException ie) {
            log.error("IOException: {}", ie.getMessage());
            loadBalanceManager.reconnectToNewServer();
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
                loadBalanceManager.reconnectToNewServer();
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
            socketManager.initializeBufferedReader(s);
        } catch (IOException e) {
            log.error("Unable to connect to server: {}", e.getMessage());
        }
    }
}