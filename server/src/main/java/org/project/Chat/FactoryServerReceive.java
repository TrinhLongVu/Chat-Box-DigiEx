package org.project.Chat;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.border.Border;

import org.project.ServerManager;

import src.lib.Client;
import src.lib.DataSave;
import src.lib.Send;
import src.lib.TypeReceive;

public class FactoryServerReceive {
    private static final Map<String, MessageHandlerFactory> factoryMap = new HashMap<>();
    static {
        factoryMap.put("login", new LoginMessageHandlerFactory());
        factoryMap.put("chat", new ChatMessageHandlerFactory());
        factoryMap.put("group", new GroupMessageHandlerFactory());
        factoryMap.put("chat-group", new ChatGroupMessageHandlerFactory());
    }

    public static MessageHandlerFactory getFactory(String type) {
        return factoryMap.get(type);
    }
}

interface MessageHandlerFactory {
    void handle(TypeReceive data, Socket socket, String message);
}

class LoginMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        if (BrokerInfo.brokerSocket == null) {
            Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE, "An error occurred: {0}",
                    " broker is not exits");
            return;
        }
        // if data is sent from client without broker
        if (!data.haveFlag()) {
            Client currentClient = new Client(data.getNameSend(), socket);
            DataSave.clients.add(currentClient);
            Receive.receiveClientMap.put(socket, currentClient);

            SendMessageSocket(BrokerInfo.brokerSocket, message + "&&flag:true");
        } else {
            SendUsersOnline.handle(null);
        }
    }

    private void SendMessageSocket(Socket reciever, String data) {
        try {
            new Send(reciever).sendData(data);
        } catch (IOException e) {
            Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE,
                    "An error occurred: {0}", e.getMessage());
        }
    }
}

class ChatMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        if (BrokerInfo.brokerSocket == null) {
            Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE, "An error occurred: {0}",
                    " broker is not exits");
            return;
        }

        if (!data.haveFlag()) {
            SendMessageSocket(BrokerInfo.brokerSocket, receiveMsg + "&&flag:true");
        } else {
            Socket receiver = findClientSocketByName(data.getNameReceive());
            if (receiver != null) {
               SendMessageSocket(receiver, "type:chat&&send:" + data.getNameSend() + "&&data:" + data.getData());
            }
            else{
                handleChatGroup(data);
            }
        }
    }
    
    private void SendMessageSocket(Socket reciever, String data) {
        try {
            new Send(reciever).sendData(data);
        } catch (IOException e) {
            Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE,
                    "An error occurred: {0}", e.getMessage());
        }
    }
    
    private Socket findClientSocketByName(String name) {
        for (Client client : DataSave.clients) {
            if (client.getName().equals(name)) {
                return client.getSocket();
            }
        }
        return null;
    }
    
    public void handleChatGroup(TypeReceive data) {

        System.out.println("handle chat group....." + data.getData() + data.getNameReceive());

        for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
            if (dataName.getKey().equals(data.getNameReceive())) {
                String[] usersInGroup = dataName.getValue().split(", ");
                for (String userInGroup : usersInGroup) {
                    if (!userInGroup.equals(data.getNameSend())) {
                        DataSave.clients.stream()
                                .filter(client -> client.getName().equals(userInGroup))
                                .forEach(client -> {
                                    try {
                                        new Send(client.getSocket()).sendData(
                                                "type:chat-group&&send:" + data.getNameSend() + ","
                                                        + data.getNameReceive()
                                                        + "&&data:" + data.getData());
                                    } catch (IOException e) {
                                        Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(
                                                Level.SEVERE,
                                                "An error occurred: {0}", e.getMessage());
                                    }
                                });
                    }
                }
            }
        }
    }
}

class GroupMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        if (BrokerInfo.brokerSocket == null) {
            Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE, "An error occurred: {0}",
                    " broker is not exits");
            return;
        }

        if (!data.haveFlag()) {
            SendMessageSocket(BrokerInfo.brokerSocket, receiveMsg + "&&flag:true");
        } else {
            DataSave.groups.put(data.getNameSend(), data.getNameReceive());
            SendUsersOnline.handle(data.getNameSend());
        }
    }
    
    private void SendMessageSocket(Socket reciever, String data) {
        try {
            new Send(reciever).sendData(data);
        } catch (IOException e) {
            Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE,
                    "An error occurred: {0}", e.getMessage());
        }
    }
}

class ChatGroupMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
            if (dataName.getKey().equals(data.getNameReceive())) {
                String[] usersInGroup = dataName.getValue().split(", ");
                for (String userInGroup : usersInGroup) {
                    DataSave.clients.stream()
                            .filter(client -> client.getName().equals(userInGroup))
                            .forEach(client -> {
                                try {
                                    new Send(client.getSocket()).sendData(
                                            "type:chat-group&&send:" + data.getNameSend() + "&&data:" + data.getData());
                                } catch (IOException e) {
                                    Logger.getLogger(ChatGroupMessageHandlerFactory.class.getName()).log(Level.SEVERE,
                                            "An error occurred: {0}", e.getMessage());
                                }
                            });
                }
            }
        }
    }
}
