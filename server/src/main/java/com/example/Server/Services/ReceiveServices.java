package com.example.Server.services;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.example.Server.controller.ReceiveController;
import com.example.Server.payloads.BrokerInfo;
import com.example.Server.utils.CallAPI;

import src.lib.Client;
import src.lib.DataSave;
import src.lib.TypeReceive;

public class ReceiveServices {
    private static final Map<String, InterfaceMessageHandler> factoryMethod = new HashMap<>();
    static {
        factoryMethod.put("login", new LoginMessageHandler());
        factoryMethod.put("chat", new ChatMessageHandler());
        factoryMethod.put("group", new GroupMessageHandler());
        factoryMethod.put("chat-group", new ChatGroupMessageHandler());
        factoryMethod.put("disconnect", new DisconnectHandler());
    }
    public static InterfaceMessageHandler getFactory(String type) {
        return factoryMethod.get(type);
    }
}
class LoginMessageHandler implements InterfaceMessageHandler {
    private static final String FLAG_TRUE = "&&flag:true";
    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        if (BrokerInfo.brokerSocket == null) {
            Logger.getLogger(ChatMessageHandler.class.getName()).log(Level.SEVERE, "An error occurred: {0}", " broker is not exits");
            return;
        }
        if (!data.isSendBroker()) SendMessageToBroker(data, socket, message);
        else SendToClient();
    }
    private void SendMessageToBroker(TypeReceive data, Socket socket, String message) {
        Client currentClient = new Client(data.getNameSend(), socket);
        DataSave.clients.add(currentClient);
        ReceiveController.receiveClientMap.put(socket, currentClient);
        SendServices.SendMessage(BrokerInfo.brokerSocket, message + FLAG_TRUE);
    }
    private void SendToClient() {
        SendServices.SendUserOnline();
    }
}
class ChatMessageHandler implements InterfaceMessageHandler {
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        if (!isExitBroker()) return;
        if (!data.isSendBroker()) {
            SendServices.SendMessage(BrokerInfo.brokerSocket, receiveMsg + "&&flag:true");
            return;
        }
        Socket receiver = findClientSocketByName(data.getNameReceive());
        if (receiver != null) SendChatToClient(receiver, data);
        else handleChatGroup(data);
    }
    private boolean isExitBroker() {
        if (BrokerInfo.brokerSocket == null) {
            Logger.getLogger(ChatMessageHandler.class.getName()).log(Level.SEVERE, "An error occurred: {0}",
                    " broker is not exits");
            return false;
        }
        return true;
    }
    private void SendChatToClient(Socket receiver, TypeReceive data) {
        SendServices.SendMessage(receiver, "type:chat&&send:" + data.getNameSend() + "&&data:" + data.getData());
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
        DataSave.groups.entrySet().stream()
            .filter(entry -> entry.getKey().equals(data.getNameReceive()))
            .map(Map.Entry::getValue)
            .flatMap(groupMembers -> Stream.of(groupMembers.split(",")))
            .filter(userInGroup -> !userInGroup.equals(data.getNameSend()))
            .forEach(userInGroup -> DataSave.clients.stream()
                .filter(client -> client.getName().equals(userInGroup))
                .forEach(client -> {
                    SendServices.SendMessage(client.getSocket(), "type:chat-group&&send:" + data.getNameSend() + ","
                            + data.getNameReceive() + "&&data:" + data.getData());
                }));
    }
}
class GroupMessageHandler implements InterfaceMessageHandler {
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        if (!isExitBroker()) return;
        if (!data.isSendBroker()) SendToBroker(receiveMsg, data);
        else SendToGroup(data);
    }
    private void SendToBroker(String receiveMsg, TypeReceive data) {
        SendServices.SendMessage(BrokerInfo.brokerSocket, receiveMsg + "&&flag:true");
        CallAPI.PostData("/create-group",
                "%group:" + data.getNameSend() + "," + data.getNameReceive() + "%&&localhost@1234");
    }

    private void SendToGroup(TypeReceive data) {
        DataSave.groups.put(data.getNameSend(), data.getNameReceive());
        SendServices.SendUserOnline();
    }
    private boolean isExitBroker() {
        if (BrokerInfo.brokerSocket == null) {
            Logger.getLogger(ChatMessageHandler.class.getName()).log(Level.SEVERE, "An error occurred: {0}",
                    " broker is not exits");
            return false;
        }
        return true;
    }
}
class ChatGroupMessageHandler implements InterfaceMessageHandler {
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
            if (dataName.getKey().equals(data.getNameReceive())) {
                String[] usersInGroup = dataName.getValue().split(",");
                for (String userInGroup : usersInGroup) {
                    DataSave.clients.stream()
                        .filter(client -> client.getName().equals(userInGroup))
                        .forEach(client -> {
                            SendServices.SendMessage(client.getSocket(), "type:chat-group&&send:" + data.getNameSend() + "&&data:" + data.getData());
                        });
                }
            }
        }
    }
}
class DisconnectHandler implements InterfaceMessageHandler {
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        SendServices.SendUserOnline();
    }
}
