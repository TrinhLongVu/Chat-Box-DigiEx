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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class Receive implements Runnable {
    private BufferedReader br;
    private Socket _socket;
    private Client currentClient;
    private static String userOnines = "[]";

    public Receive(Socket socket) {
        this._socket = socket;
        try {
            InputStream is = socket.getInputStream();
            this.br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            throw new RuntimeException("Error initializing BufferedReader", e);
        }
    }

    public static void sendUserOnline() {
        String resultSend = "";
        for (Client client : DataSave.clients) {
            resultSend = "type:online&&data:" + getAllExceptMe(userOnines, client.getName());

            for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
                String[] usersInGroup = dataName.getValue().split(", ");
                if (List.of(usersInGroup).contains(client.getName())) {
                    resultSend = resultSend.substring(0, resultSend.length() - 1) + ", " + dataName.getKey() + "]";
                }
            }

            new Send(client.getSocket()).sendData(resultSend);
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

                switch (data.getType()) {
                    case "login":
                        handleLogin(data);
                        break;
                    case "chat":
                        handleChat(data);
                        break;
                    case "group":
                        handleGroup(data);
                        break;
                    case "chat-group":
                        handleChatGroup(data);
                        break;
                    case "users": {
                        userOnines = data.getData();
                        sendUserOnline();
                        break;
                    }
                    default:
                        System.out.println("Type not found: " + data.getType());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from socket: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleLogin(TypeReceive data) {
        currentClient = new Client(data.getNameSend(), _socket);
        DataSave.clients.add(currentClient);
        sendUserOnline();
    }

    private void handleChat(TypeReceive data) {
        for (Client client : DataSave.clients) {
            if (client.getName().equals(data.getNameReceive())) {
                new Send(client.getSocket()).sendData(
                        "type:chat&&send:" + data.getNameSend() + "&&data:" + data.getData());
            }
        }

        for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
            if (dataName.getKey().equals(data.getNameReceive())) {
                String[] usersInGroup = dataName.getValue().split(", ");
                for (String userInGroup : usersInGroup) {
                    if (!userInGroup.equals(data.getNameSend())) {
                        DataSave.clients.stream()
                                .filter(client -> client.getName().equals(userInGroup))
                                .forEach(client -> new Send(client.getSocket()).sendData(
                                        "type:chat-group&&send:" + data.getNameSend() + "," + data.getNameReceive() + "&&data:" + data.getData()));
                    }
                }
            }
        }
    }

    private void handleGroup(TypeReceive data) {
        DataSave.groups.put(data.getNameSend(), data.getNameReceive());
        sendUserOnline();
    }

    private void handleChatGroup(TypeReceive data) {
        for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
            if (dataName.getKey().equals(data.getNameReceive())) {
                String[] usersInGroup = dataName.getValue().split(", ");
                for (String userInGroup : usersInGroup) {
                    DataSave.clients.stream()
                            .filter(client -> client.getName().equals(userInGroup))
                            .forEach(client -> new Send(client.getSocket()).sendData(
                                    "type:chat-group&&send:" + data.getNameSend() + "&&data:" + data.getData()));
                }
            }
        }
    }

    private void cleanup() {
        try {
            System.out.println("Closing connection....");
            if (_socket != null && !_socket.isClosed()) {
                _socket.close();
            }
            if (currentClient != null) {
                DataSave.clients.remove(currentClient);
                sendUserOnline();
                System.out.println(
                        "Client " + currentClient.getName() + " disconnected and removed from active clients.");
            }
        } catch (IOException e) {
            System.out.println("Error closing client socket: " + e.getMessage());
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

