package project.Chat;

import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import project.View.HomePage;
import src.lib.DataSave;
import src.lib.TypeReceive;

public class FactoryClientReceive {
    private static final Map<String, MessageHandlerFactory> factoryMap = new HashMap<>();
    static {
        factoryMap.put("online", new UpdateUserOnlineMessageHandlerFactory());
        factoryMap.put("chat", new ChatMessageHandlerFactory());
        factoryMap.put("chat-group", new ChatGroupMessageHandlerFactory());
        factoryMap.put("error", new ErrorMessageHandlerFactory());
    }

    public static MessageHandlerFactory getFactory(String type) {
        return factoryMap.get(type);
    }
}

interface MessageHandlerFactory {
    default void handle(TypeReceive data, Socket socket, String message){};
}

class UpdateUserOnlineMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        String newUser = data.getData();
        DataSave.userOnline.add(newUser);

        SwingUtilities.invokeLater(() -> {
            HomePage.listModelUsers.clear();
            for (String user : DataSave.userOnline) {
                HomePage.listModelUsers.addElement(user);
            }
        });
    }
}

class ChatMessageHandlerFactory implements MessageHandlerFactory {
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
                HomePage.listModel.clear();
                for (String hist : finalHistory) {
                    HomePage.listModel.addElement(hist);
                }
            });
        }
    }
}

class ChatGroupMessageHandlerFactory implements MessageHandlerFactory {
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
                HomePage.listModel.clear();
                for (String hist : finalHistory) {
                    HomePage.listModel.addElement(hist);
                }
            });
        }
    }
}



class ErrorMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        System.out.println("error: " + data.getData());
        JOptionPane.showMessageDialog(null, "error: " + data.getData());
    }
}