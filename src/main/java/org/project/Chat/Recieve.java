package org.project.Chat;

import org.project.Data.Client;
import org.project.payload.TypeRecieve;
import org.project.Utils.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

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
                System.out.println("Received : " + receiveMsg);
                TypeRecieve data = helper.FormatData(receiveMsg);

                switch (data.getType()) {
                    case "login": {
                        Client newClient = new Client(data.getIdSend(), _socket);
                        System.out.println("login success:::::" + newClient.getName());

                        Client.clients.add(newClient);
                    }
                    case "chat": {
                        for(Client client : Client.clients) {
                            if(client.getName().equals(data.getIdRecieve())) {
                                new Send(client.getSocket()).sendData(data.getData());
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
