package project.Chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;

import project.LoadBalancer;
import project.ServerManagerInfo;
import project.ServerManagerUI;
import src.lib.Helper;
import src.lib.Send;
import src.lib.TypeReceive;

public class Receive implements Runnable {
    private String receiveMsg = "";
    private BufferedReader br;
    private Socket socket;
    private ServerInfo availableServer;

    public Receive(Socket ss, ServerInfo availableServer) {
        this.availableServer = availableServer;
        this.socket = ss;
        InputStream is;
        try {
            is = ss.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void receiveData() {
        try {
            this.receiveMsg = this.br.readLine();
            if (receiveMsg != null) {
                System.out.println("Received: " + receiveMsg);
                TypeReceive data = Helper.FormatData(receiveMsg);

                switch (data.getType()) {
                    case "login": {
                        Database.clients.add(new ClientInfo(data.getNameSend(), availableServer.toString()));
                        updateUserOnline();
                        return;
                    }
                    default:
                        System.out.println("Received invalid data: " + receiveMsg);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateUserOnline() {
        for (ServerInfo server : Database.serverList) {
            List<String> names = Database.clients.stream()
                    .map(ClientInfo::getName)
                    .collect(Collectors.toList());

            try {
                new Send(server.getSocket()).sendData("type:users&&data:" + names.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        try {
            while ((receiveMsg = br.readLine()) != null) {
                System.out.println("message::::" + receiveMsg);
                TypeReceive data = Helper.FormatData(receiveMsg);

                if (data == null) {
                    System.out.println("Received invalid data: " + receiveMsg);
                    continue;
                }

                switch (data.getType()) {
                    case "chat": {
                        for (ClientInfo client : Database.clients) {
                            if (client.getName().equals(data.getNameReceive())) {
                                for (ServerInfo server : Database.serverList) {
                                    if (client.getServerinfo().equals(server.toString())) {
                                        new Send(server.getSocket()).sendData(receiveMsg);
                                    }
                                }
                            }
                        }
                        break;
                    }

                    case "disconnect": {
                        // Using removeIf to remove the client
                        Database.clients.removeIf(client -> {
                            if (client.getName().equals(data.getNameSend())) {
                                for (ServerInfo server : Database.serverList) {
                                    if (client.getServerinfo().equals(server.toString())) {
                                        server.decrementClients();
                                        if (server.getActiveClients() == 0) {
                                            for (ServerManagerInfo serverManagerInfo : Database.serverManagerInfoList) {
                                                if (serverManagerInfo.getPort() == server.getPort()) {
                                                    serverManagerInfo.setIsRunning(false);
                                                    updateUI();
                                                }
                                            }
                                        }
                                    }
                                }
                                return true; // Remove the client
                            }
                            return false; // Do not remove the client
                        });

                        System.out.print(Database.clients.toString());
                        updateUserOnline();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from socket: " + e.getMessage());
        }
    }

    private void updateUI() {
        SwingUtilities.invokeLater(() -> {
            ServerManagerUI ui = LoadBalancer.getUI();
            if (ui != null) {
                ui.updateServerList(Database.serverManagerInfoList);
            }
        });
    }
}
