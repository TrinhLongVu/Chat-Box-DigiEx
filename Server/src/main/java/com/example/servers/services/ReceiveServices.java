package com.example.servers.services;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.servers.payloads.BrokerInfo;
import com.example.servers.utils.CallAPI;
import com.example.support.*;

@Component
public class ReceiveServices {
    private static final Map<String, InterfaceMessageHandler> factoryMethod = new HashMap<>();

    @Autowired
    private LoginMessageHandler loginMessageHandler;
    @Autowired
    private ChatMessageHandler chatMessageHandler;
    @Autowired
    private GroupMessageHandler groupMessageHandler;
    @Autowired
    private ChatGroupMessageHandler chatGroupMessageHandler;
    @Autowired
    private DisconnectHandler disconnectHandler;

    @PostConstruct
    private void init() {
        factoryMethod.put("login", loginMessageHandler);
        factoryMethod.put("chat", chatMessageHandler);
        factoryMethod.put("group", groupMessageHandler);
        factoryMethod.put("chat-group", chatGroupMessageHandler);
        factoryMethod.put("disconnect", disconnectHandler);
    }

    public InterfaceMessageHandler getFactory(String type) {
        return factoryMethod.get(type);
    }
}

@Component
class LoginMessageHandler implements InterfaceMessageHandler {
    private static final String FLAG_TRUE = "&&flag:true";
    private final Logger log = LogManager.getLogger(LoginMessageHandler.class);

    @Autowired
    private SendServices sendServices;

    @Override
    public void handle(TypeReceive data, Socket socket, String message) {
        if (BrokerInfo.brokerSocket == null) {
            log.warn("An error occurred: {0} broker is not exits");
            return;
        }
        if (!data.isSendBroker())
            SendMessageToBroker(data, socket, message);
        else
            SendToClient();
    }

    private void SendMessageToBroker(TypeReceive data, Socket socket, String message) {
        Client currentClient = new Client(data.getNameSend(), socket);
        DataSave.clients.add(currentClient);
        sendServices.SendMessage(BrokerInfo.brokerSocket, message + FLAG_TRUE);
    }

    private void SendToClient() {
        sendServices.SendUserOnline();
    }
}

@Component
class ChatMessageHandler implements InterfaceMessageHandler {
    @Autowired
    private SendServices sendServices;
    private final Logger log = LogManager.getLogger(ChatMessageHandler.class);

    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        if (!isExitBroker())
            return;
        if (!data.isSendBroker()) {
            sendServices.SendMessage(BrokerInfo.brokerSocket, receiveMsg + "&&flag:true");
            return;
        }
        Socket receiver = findClientSocketByName(data.getNameReceive());
        if (receiver != null)
            SendChatToClient(receiver, data);
        else
            handleChatGroup(data);
    }

    private boolean isExitBroker() {
        if (BrokerInfo.brokerSocket == null) {
            log.warn("An error occurred: {0} broker is not exits");
            return false;
        }
        return true;
    }

    private void SendChatToClient(Socket receiver, TypeReceive data) {
        sendServices.SendMessage(receiver, "type:chat&&send:" + data.getNameSend() + "&&data:" + data.getData());
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
                            sendServices.SendMessage(client.getSocket(),
                                    "type:chat-group&&send:" + data.getNameSend() + ","
                                            + data.getNameReceive() + "&&data:" + data.getData());
                        }));
    }
}

@Component
class GroupMessageHandler implements InterfaceMessageHandler {
    private final Logger log = LogManager.getLogger(GroupMessageHandler.class);
    @Autowired
    private CallAPI callAPI;

    @Autowired
    private SendServices sendServices;

    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        if (!isExitBroker())
            return;
        if (!data.isSendBroker())
            SendToBroker(receiveMsg, data);
        else
            SendToGroup(data);
    }

    private void SendToBroker(String receiveMsg, TypeReceive data) {
        sendServices.SendMessage(BrokerInfo.brokerSocket, receiveMsg + "&&flag:true");
        callAPI.PostData("/create-group",
                "%group:" + data.getNameSend() + "," + data.getNameReceive() + "%&&localhost@1234");
    }

    private void SendToGroup(TypeReceive data) {
        DataSave.groups.put(data.getNameSend(), data.getNameReceive());
        sendServices.SendUserOnline();
    }

    private boolean isExitBroker() {
        if (BrokerInfo.brokerSocket == null) {
            log.warn("An error occurred: {0}  broker is not exits");
            return false;
        }
        return true;
    }
}

@Component
class ChatGroupMessageHandler implements InterfaceMessageHandler {
    @Autowired
    private SendServices sendServices;
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
            if (dataName.getKey().equals(data.getNameReceive())) {
                String[] usersInGroup = dataName.getValue().split(",");
                for (String userInGroup : usersInGroup) {
                    DataSave.clients.stream()
                            .filter(client -> client.getName().equals(userInGroup))
                            .forEach(client -> {
                                sendServices.SendMessage(client.getSocket(),
                                        "type:chat-group&&send:" + data.getNameSend() + "&&data:" + data.getData());
                            });
                }
            }
        }
    }
}
@Component
class DisconnectHandler implements InterfaceMessageHandler {
    @Autowired
    private SendServices sendServices;
    @Override
    public void handle(TypeReceive data, Socket socket, String receiveMsg) {
        sendServices.SendUserOnline();
    }
}