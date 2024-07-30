package project.Chat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


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
                Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());

            }
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

                    case "server-send-clients": {
                        String name_and_port[] = data.getNameSend().split(",");

                        for (ServerInfo server : Database.serverList) {
                            if (server.getPort() == Integer.valueOf(name_and_port[1])) {
                                System.out.print("oke....");
                                Database.clients.add(new ClientInfo(name_and_port[0], server.toString()));
                                updateUserOnline();
                            }
                        }

                        break;
                    }

                    case "disconnect": {
                        Iterator<ClientInfo> iterator = Database.clients.iterator();
                        while (iterator.hasNext()) {
                            ClientInfo client = iterator.next();
                            if (client.getName().equals(data.getNameSend())) {
                                iterator.remove(); // Safe removal using iterator
                                for (ServerInfo server : Database.serverList) {
                                    if (client.getServerinfo().equals(server.toString())) {
                                        server.decrementClients();
                                    }
                    
                                    if (server.getActiveClients() == 0) {
                                        server.getSocket().close();
                                        for (ServerManagerInfo serverManager : Database.oldServerManager) {
                                            if (serverManager.getPort() == server.getPort()) {
                                                Database.serverList.remove(server);
                                                updateUserOnline();
                                                serverManager.setOpenning(false);
                                                serverManager.getServerManager().stopServer();
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        System.out.print(Database.clients.toString());
                        updateUserOnline();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from socket: " + e.getMessage());
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Error reading from socket: {0}", e.getMessage());

        }
    }
}
