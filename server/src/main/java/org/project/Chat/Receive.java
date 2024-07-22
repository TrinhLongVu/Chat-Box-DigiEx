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
import java.util.stream.Collectors;

public class Receive extends Thread {
    String receiveMsg = "";
    BufferedReader br;
    private Socket _socket;
    private Client currentClient;

    public Receive(Socket ss) {
        InputStream is = null;
        try {
            is = ss.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        br = new BufferedReader(new InputStreamReader(is));
        _socket = ss;
    }

    public static void sendUserOnline() {
        String resultSend = "";
        for(Client client: DataSave.clients) {
            List<String> names = DataSave.clients.stream()
                    .filter((c) -> !c.getName().equals(client.getName()))
                    .map((clientName) -> clientName.getName())
                    .collect(Collectors.toList());
            resultSend = "type:online&&data:" + names.toString();

            for (Map.Entry<String, String> dataName : DataSave.groups.entrySet())  {
                String usersInGroup[] = dataName.getValue().split(", ");
                for(String userInGroup: usersInGroup) {
                    if(userInGroup.equals(client.getName())) {
                        resultSend = resultSend.substring(0, resultSend.length() - 1) + ", " + dataName.getKey() + "]";
                    }
                }
            }

            new Send(client.getSocket()).sendData(resultSend);
        }
    }

    public void run() {
        try {
            do {
                this.receiveMsg = this.br.readLine();
                System.out.println("message::::" + receiveMsg);
                TypeReceive data = null;
                if(this.receiveMsg != null) {
                    data = Helper.FormatData(receiveMsg);
                }

                if (data == null) {
                    System.out.println("Received invalid data: " + receiveMsg);
                    break;
                }

                switch (data.getType()) {
                    case "login": {
                        currentClient = new Client(data.getNameSend(), _socket);
                        DataSave.clients.add(currentClient);
                        sendUserOnline();
                        break;
                    }
                    case "chat": {
                        for (Client client : DataSave.clients) {
                            if (client.getName().equals(data.getNameReceive())) {
                                new Send(client.getSocket()).sendData(
                                        "type:chat&&send:" + data.getNameSend() + "&&data:" + data.getData());
                            }
                        }

                        for (Map.Entry<String, String> dataName : DataSave.groups.entrySet())  {
                            // check in group
                            if(dataName.getKey().equals(data.getNameReceive())) {
                                String usersInGroup[] = dataName.getValue().split(", ");
                                for(String userInGroup: usersInGroup) {
                                    for (Client client : DataSave.clients) {
                                        if (client.getName().equals(userInGroup) && !client.getName().equals(data.getNameSend())) {
                                            new Send(client.getSocket()).sendData(
                                                    "type:chat-group&&send:" + data.getNameSend() + "," + data.getNameReceive() + "&&data:" + data.getData());
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case "group": {
                        DataSave.groups.put(data.getNameSend(), data.getNameReceive());
                        sendUserOnline();
                        break;
                    }

                    case "chat-group": {
                        for (Map.Entry<String, String> dataName : DataSave.groups.entrySet())  {
                            // check in group
                            if(dataName.getKey().equals(data.getNameReceive())) {
                                String usersInGroup[] = dataName.getValue().split(", ");
                                for(String userInGroup: usersInGroup) {
                                    for (Client client : DataSave.clients) {
                                        if (client.getName().equals(userInGroup)) {
                                            new Send(client.getSocket()).sendData(
                                                    "type:chat-group&&send:" + data.getNameSend() + "&&data:" + data.getData());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    default: {
                        System.out.println("Type not found");
                        break;
                    }
                }
            }while (true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            cleanup();
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
                System.out.println("Client " + currentClient.getName() + " disconnected and removed from active clients.");
            }
        } catch (IOException e) {
            System.out.println("Error closing client socket: " + e.getMessage());
        }
    }
}
