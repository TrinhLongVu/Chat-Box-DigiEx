package org.project.Chat;

import src.lib.DataSave;
import src.lib.Client;
import src.lib.TypeReceive;
import src.lib.Helper;
import src.lib.Send;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class balancer {
    public static Socket loadBalanSocket = null;
}

public class Receive implements Runnable {
    private BufferedReader br;
    private Socket socket;
    private Client currentClient;
    private static String userOnines = "[]";

    public Receive(Socket socket) {
        this.socket = socket;
        try {
            InputStream is = socket.getInputStream();
            this.br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            throw new RuntimeException("Error initializing BufferedReader", e);
        }
    }

    @Override
    public void run() {
        String receiveMsg;
        try {
            while ((receiveMsg = br.readLine()) != null) {
                System.out.println("message::::" + receiveMsg);
                TypeReceive data = Helper.FormatData(receiveMsg);

                if (data == null) {
                    System.out.println("Received invalid data: " + receiveMsg);
                    continue;
                }

                if (data.getType().equals("users")) {
                    userOnines = data.getData();
                    SendUserOnlines.handle(userOnines);;
                    continue;
                }
                MessageHandlerFactory factory = FactoryReceive.getFactory(data.getType());
                if (factory != null) {
                    factory.handle(data, socket, userOnines, receiveMsg);
                } else {
                    System.out.println("Received invalid data: " + data);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from socket: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            System.out.println("Closing connection....");
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (currentClient != null) {
                DataSave.clients.remove(currentClient);
                SendUserOnlines.handle(userOnines);
                System.out.println(
                        "Client " + currentClient.getName() + " disconnected and removed from active clients.");
                
                        try {
                            new Send(balancer.loadBalanSocket).sendData("type:disconnect&&send:" + currentClient.getName());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
            }
        } catch (IOException e) {
            System.out.println("Error closing client socket: " + e.getMessage());
        }
    }
}

class FactoryReceive {
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
    default void handle(TypeReceive data, Socket socket, String userOnines, String message){};
}

class SendUserOnlines {
    public static void handle(String userOnines) {
        String resultSend = "";
        for (Client client : DataSave.clients) {
            resultSend = "type:online&&data:" + getAllExceptMe(userOnines, client.getName());

            for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
                String[] usersInGroup = dataName.getValue().split(", ");
                if (List.of(usersInGroup).contains(client.getName())) {
                    resultSend = resultSend.substring(0, resultSend.length() - 1) + ", " + dataName.getKey() + "]";
                }
            }

            try {
                new Send(client.getSocket()).sendData(resultSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getAllExceptMe(String listUserOnline, String myName) {
        String regex = "\\b" + myName + "\\b,?\\s?";
        String result = listUserOnline.replaceAll(regex, "");
        result = result.replaceAll(",\\s*\\]", "]");
        result = result.replaceAll("\\[\\s*\\]", "[]");
        return result;
    }
}

class loadBalancerMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String userOnines, String message) {
        balancer.loadBalanSocket = socket;
    }
}

class LoginMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String userOnines, String message) {
        Client currentClient = new Client(data.getNameSend(), socket);
        DataSave.clients.add(currentClient);
        SendUserOnlines.handle(userOnines);
    }
}

class ChatMessageHandlerFactory implements MessageHandlerFactory {
    @Override
    public void handle(TypeReceive data, Socket socket, String userOnines, String receiveMsg) {
        Socket receiver = null;
        for (Client client : DataSave.clients) {
            if (client.getName().equals(data.getNameReceive())) {
                receiver = client.getSocket();
            }
        }

        try {
            // if receive have in server then send else send message to load balancer
            if (receiver != null) {
                new Send(receiver).sendData(
                    "type:chat&&send:" + data.getNameSend() + "&&data:" + data.getData());
            } else {
                new Send(balancer.loadBalanSocket).sendData(receiveMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                                        "type:chat-group&&send:" + data.getNameSend() + "," + data.getNameReceive()
                                            + "&&data:" + data.getData());
                                } catch (IOException e) {
                                    e.printStackTrace();
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
    public void handle(TypeReceive data, Socket socket, String userOnines, String receiveMsg) {
        DataSave.groups.put(data.getNameSend(), data.getNameReceive());
        SendUserOnlines.handle(userOnines);
    }
}

class ChatGroupMessageHandlerFactory implements MessageHandlerFactory {
    @Override 
    public void handle(TypeReceive data, Socket socket, String userOnines, String receiveMsg) {
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
                                    e.printStackTrace();
                                }
                            }
                        );
                }
            }
        }
    }
}