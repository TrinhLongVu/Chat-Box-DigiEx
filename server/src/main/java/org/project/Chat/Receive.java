package org.project.Chat;

import org.project.Data.Client;
import org.project.Data.DataSave;
import org.project.payload.TypeReceive;
import org.project.Utils.helper;

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

    public void run() {
        try {
            do {
                this.receiveMsg = this.br.readLine();
                TypeReceive data = helper.FormatData(receiveMsg);
                System.out.println("message::::" + receiveMsg);

                switch (data.getType()) {
                    case "login": {
                        Client newClient = new Client(data.getNameSend(), _socket);
                        DataSave.clients.add(newClient);
                        helper.sendUserOnline();
                        break;
                    }
                    case "chat": {
                        for (Client client : DataSave.clients) {
                            if (client.getName().equals(data.getNameReceive())) {
                                new Send(client.getSocket()).sendData(
                                        "type:chat&&send:" + data.getNameSend() + "&&content:" + data.getData());
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
                                                    "type:chat-group&&send:" + data.getNameSend() + "," + data.getNameReceive() + "&&content:" + data.getData());
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case "group": {
                        DataSave.groups.put(data.getNameSend(), data.getNameReceive());
                        helper.sendUserOnline();
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
                                                    "type:chat-group&&send:" + data.getNameSend() + "&&content:" + data.getData());
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
    }
}
