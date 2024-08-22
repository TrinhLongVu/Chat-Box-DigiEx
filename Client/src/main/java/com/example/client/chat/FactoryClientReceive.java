package com.example.client.chat;

import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import com.example.client.core.ClientInfo;
import com.example.client.view.HomePage;
import com.example.Support.DataSave;
import com.example.Support.TypeReceive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

@Component
public class FactoryClientReceive {
    private final Map<String, MessageHandlerFactory> factoryMap = new HashMap<>();

    @Autowired
    private UpdateUserOnlineMessageHandlerFactory updateUserOnlineMessageHandlerFactory;

    @Autowired
    private ChatMessageHandlerFactory chatMessageHandlerFactory;

    @Autowired
    private ChatGroupMessageHandlerFactory chatGroupMessageHandlerFactory;

    @Autowired
    private ErrorMessageHandlerFactory errorMessageHandlerFactory;

    @PostConstruct
    private void init() {
        factoryMap.put("online", updateUserOnlineMessageHandlerFactory);
        factoryMap.put("chat", chatMessageHandlerFactory);
        factoryMap.put("chat-group", chatGroupMessageHandlerFactory);
        factoryMap.put("error", errorMessageHandlerFactory);
    }

    public MessageHandlerFactory getFactory(String type) {
        return factoryMap.get(type);
    }
}

interface MessageHandlerFactory {
    default void handle(TypeReceive data, Socket socket, String message){};
}

@Component
@RequiredArgsConstructor
class UpdateUserOnlineMessageHandlerFactory implements MessageHandlerFactory {
    private final ClientInfo clientInfo;

    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        String dataReceive = data.getData();
        String[] userOnlines = dataReceive.split(",");
        DataSave.userOnline.clear();
        for (String userOnline : userOnlines) {
            if (!userOnline.equals(clientInfo.getUserName())) {
                DataSave.userOnline.add(userOnline);
            }
        }

        SwingUtilities.invokeLater(() -> {
            clientInfo.getClientList().clear();
            for (String userOnline : DataSave.userOnline) {
                String user[] = userOnline.split("\\?");
                clientInfo.getClientList().addElement(user[0]);
            }
        });
    }
}

@Component
@RequiredArgsConstructor
class ChatMessageHandlerFactory implements MessageHandlerFactory {
    private final ClientInfo clientInfo;
    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        String content = data.getData();
        String userSend = data.getNameSend();
        LinkedList<String> history = DataSave.contentChat.get(userSend);
        if (history == null) {
            history = new LinkedList<>();
            DataSave.contentChat.put(userSend, history);
        }
        history.add(userSend + ": " + content);
        final LinkedList<String> finalHistory = history;
        if (DataSave.selectedUser.equals(userSend)) {
            SwingUtilities.invokeLater(() -> {
                clientInfo.getMessageList().clear();
                for (String hist : finalHistory) {
                    clientInfo.getMessageList().addElement(hist);
                }
            });
        }
    }
}

@Component
@RequiredArgsConstructor
class ChatGroupMessageHandlerFactory implements MessageHandlerFactory {
    private final ClientInfo clientInfo;
    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        String content = data.getData();
        String userSendCombined = data.getNameSend();
        String[] userSend = userSendCombined.split(",");
        LinkedList<String> history = DataSave.contentChat.get(userSend[1]);
        if (history == null) {
            history = new LinkedList<>();
            DataSave.contentChat.put(userSend[1], history);
        }
        history.add(userSend[0] + ": " + content);
        final LinkedList<String> finalHistory = history;
        if (DataSave.selectedUser.equals(userSend[1])) {
            SwingUtilities.invokeLater(() -> {
                clientInfo.getMessageList().clear();
                for (String hist : finalHistory) {
                    clientInfo.getMessageList().addElement(hist);
                }
            });
        }
    }
}

@Component
@RequiredArgsConstructor
class ErrorMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        JOptionPane.showMessageDialog(null, "Error: " + data.getData());
    }
}