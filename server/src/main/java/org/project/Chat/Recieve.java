package org.project.Chat;

import org.project.Data.Client;
import org.project.payload.TypeRecieve;
import org.project.Utils.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

public class Recieve extends Thread {
    String receiveMsg = "";
    BufferedReader br;
    private Socket _socket;

    public Recieve(Socket ss) {
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
                TypeRecieve data = helper.FormatData(receiveMsg);

                switch (data.getType()) {
                    case "login": {
                        Client newClient = new Client(data.getNameSend(), _socket);
                        Client.clients.add(newClient);

                        for(Client client: Client.clients) {
                            List<String> names = Client.clients.stream()
                                    .filter((c) -> !c.getName().equals(client.getName()))
                                    .map((clientName) -> clientName.getName())
                                    .collect(Collectors.toList());
                            new Send(client.getSocket()).sendData("type:online&&content:" + names.toString());
                        }

                    }
                    case "chat": {
                        for(Client client : Client.clients) {
                            if(client.getName().equals(data.getNameRecieve())) {
                                new Send(client.getSocket()).sendData("type:chat&&send:" + data.getNameSend() + "&&content:" + data.getData());
                            }
                        }
                    }
                }

                System.out.println(data.getType());

            }while (true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
