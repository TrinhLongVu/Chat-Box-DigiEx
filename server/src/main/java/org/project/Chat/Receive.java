package org.project.Chat;

import org.project.Data.Client;
import org.project.Data.DataSave;
import org.project.payload.TypeReceive;
import org.project.Utils.Helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
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
                TypeReceive data = Helper.FormatData(receiveMsg);
                System.out.println(receiveMsg);

                switch (data.getType()) {
                    case "login": {
                        Client newClient = new Client(data.getNameSend(), _socket);
                        DataSave.clients.add(newClient);

                        for(Client client: DataSave.clients) {
                            List<String> names = DataSave.clients.stream()
                                    .filter((c) -> !c.getName().equals(client.getName()))
                                    .map((clientName) -> clientName.getName())
                                    .collect(Collectors.toList());
                            new Send(client.getSocket()).sendData("type:online&&content:" + names.toString());
                        }
                        break;

                    }
                    case "chat": {
                        for (Client client : DataSave.clients) {
                            if (client.getName().equals(data.getNameReceive())) {
                                new Send(client.getSocket()).sendData(
                                        "type:chat&&send:" + data.getNameSend() + "&&content:" + data.getData());
                            }
                        }
                        break;
                    }
                    case "group": {
                        DataSave.groups.put(data.getNameSend(), data.getNameReceive());

                        String resultSend = "";
                        for(Client client: DataSave.clients) {
                            List<String> names = DataSave.clients.stream()
                                    .filter((c) -> !c.getName().equals(client.getName()))
                                    .map((clientName) -> clientName.getName())
                                    .collect(Collectors.toList());
                            resultSend = "type:online&&content:" + names.toString();
                        }
                        String users[] = data.getNameReceive().split(",");

                        for(String user: users) {
                            System.out.println("send user::::" + user);
                            for(Client client : DataSave.clients) {
                                if(user.equals(client.getName())) {
                                    resultSend = resultSend.substring(0, resultSend.length() - 1) + "," + data.getNameSend() + "]";
                                    new Send(client.getSocket()).sendData(resultSend);
                                }
                            }
                        }
                        break;

                    }
                    
                    default: {
                        System.out.println("Type not found");
                        break;
                    }
                }

                System.out.println(data.getType());

            }while (true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
