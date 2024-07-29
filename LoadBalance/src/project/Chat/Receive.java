package project.Chat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.crypto.Data;

import src.lib.Helper;
import src.lib.Send;
import src.lib.TypeReceive;

public class Receive implements Runnable{
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
                Logger.getLogger(Receive.class.getName()).log(null, "An error occurred: {0}", e.getMessage());

            }
        }
    }

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
                        Iterator<ClientInfo> iterator = Database.clients.iterator();
                        while (iterator.hasNext()) {
                            ClientInfo client = iterator.next();
                            if (client.getName().equals(data.getNameSend())) {
                                for (ServerInfo server : Database.serverList) {
                                    Database.clients.remove(client);
                                    if (client.getServerinfo().equals(server.toString())) {
                                        server.decrementClients();
                                    }
                                    

                                    if (server.getActiveClients() == 0) {
                                        server.getSocket().close();
                                        Database.serverList.remove(server);
                                        for (ServerManagerInfo serverManager : Database.oldServerManager) {
                                            if (serverManager.getPort() == server.getPort()) {
                                                serverManager.setOpenning(false);
                                                System.out.print(serverManager.getOpenning());
                                                serverManager.getServerManager().stopServer();
                                                System.out.print(Database.clients.toString());
                                                updateUserOnline();
                                                return;
                                            }
                                        }
                                    }
                                }
                                iterator.remove(); // Safe removal
                            }
                        }
                        System.out.print(Database.clients.toString());
                        updateUserOnline();
                    }
                    
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from socket: " + e.getMessage());
            Logger.getLogger(Receive.class.getName()).log(null, "Error reading from socket: {0}", e.getMessage());

        }
    }
}
