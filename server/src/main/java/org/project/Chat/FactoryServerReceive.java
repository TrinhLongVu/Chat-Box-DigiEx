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
        factoryMap.put("load-balancer", new loadBalancerMessageHandlerFactory());
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
    default void handle(TypeReceive data, Socket socket, String userOnlines, String message){};
}



class loadBalancerMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String userOnlines, String message) {
        // balancer.loadBalanSocket = socket;
    }
}

class LoginMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String userOnlines, String message) {
        if (BrokerInfo.brokerSocket == null) {
            Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE, "An error occurred: {0}", " broker is not exits");
        } else {
            if (!data.haveFlag()) {
                Client currentClient = new Client(data.getNameSend(), socket);
                DataSave.clients.add(currentClient);
                Receive.receiveClientMap.put(socket, currentClient);
                try {
                    new Send(BrokerInfo.brokerSocket).sendData(message + "&&flag:true" );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                SendUsersOnline.handle(data.getNameSend());
            }
        }
    }
}

class ChatMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String userOnlines, String receiveMsg) {
        if (BrokerInfo.brokerSocket == null) {
            Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE, "An error occurred: {0}", " broker is not exits");
        } else {
            if (!data.haveFlag()) {
                try {
                    new Send(BrokerInfo.brokerSocket).sendData(receiveMsg + "&&flag:true" );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Socket receiver = null;
                for (Client client : DataSave.clients) {
                    if (client.getName().equals(data.getNameReceive())) {
                        receiver = client.getSocket();
                    }
                }
                try {
                    new Send(receiver).sendData(
                        "type:chat&&send:" + data.getNameSend() + "&&data:" + data.getData());
                } catch (IOException e) {
                    Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
                }
            }
        }

        // group handle later
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
                                        Logger.getLogger(ChatMessageHandlerFactory.class.getName()).log(Level.SEVERE,
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
    public void handle(TypeReceive data, Socket socket, String userOnlines, String receiveMsg) {
        DataSave.groups.put(data.getNameSend(), data.getNameReceive());
        SendUsersOnline.handle(data.getNameSend());
    }
}

class ChatGroupMessageHandlerFactory implements MessageHandlerFactory {
    @Override 
    public void handle(TypeReceive data, Socket socket, String userOnlines, String receiveMsg) {
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
                            }
                        );
                }
            }
        }
    }
}
