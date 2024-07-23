package project.Chat;

import src.lib.TypeReceive;
import project.View.HomePage;
import project.View.LoginForm;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import src.lib.DataSave;
import src.lib.Helper;
import src.lib.Send;

public class Receive extends Thread {
    private String receiveMsg = "";
    private BufferedReader br;
    private Socket socket;

    public Receive(Socket ss) {
        this.socket = ss;
        InputStream is;
        try {
            is = ss.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {
            while (true) {
                this.receiveMsg = this.br.readLine();
                if (receiveMsg != null) {
                    System.out.println("Received: " + receiveMsg);
                    TypeReceive data = Helper.FormatData(receiveMsg);

                    switch (data.getType()){
                        case "online": {
                            String content = data.getData();
                            String[] namesArray = content.substring(content.indexOf("[") + 1, content.indexOf("]")).split("\\s*,\\s*");
                            List<String> namesList = Arrays.asList(namesArray);
                            DataSave.userOnline = namesList;

                            HomePage.listModelUsers.clear();
                            for (String user : DataSave.userOnline) {
                                HomePage.listModelUsers.addElement(user);
                            }

                            System.out.println("user selected" + DataSave.selectedUser);
                            break;
                        }
                        case "chat": {
                            String content = data.getData();
                            String userSend = data.getNameSend();
                            LinkedList history = DataSave.contentChat.get(userSend);
                            if(history == null){
                                history = new LinkedList<>();
                                DataSave.contentChat.put(userSend, history);
                            }
                            history.add(userSend + ": " + content);
                            if(DataSave.selectedUser.equals(userSend)) {
                                HomePage.listModel.clear();
                                for(Object hist: history) {
                                    HomePage.listModel.addElement((String)hist);
                                }
                            }
                            break;
                        }
                        case "chat-group": {
                            String content = data.getData();
                            String userSend[] = data.getNameSend().split(",");

                            LinkedList history = DataSave.contentChat.get(userSend[1]);
                            if (history == null) {
                                history = new LinkedList<>();
                                DataSave.contentChat.put(userSend[1], history);
                            }
                            history.add(userSend[0] + ": " + content);
                            if (DataSave.selectedUser.equals(userSend[1])) {
                                HomePage.listModel.clear();
                                for (Object hist : history) {
                                    HomePage.listModel.addElement((String) hist);
                                }
                            }
                            break;
                        }
                        case "server": {
                            String[] hostAndPort = data.getData().split("@");
                            System.out.println(hostAndPort[0] + "...." + hostAndPort[1]);
                        
                            int port;
                            String host = hostAndPort[0];
                        
                            try {
                                port = Integer.parseInt(hostAndPort[1]);
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid port number format");
                                return;
                            }
                        
                            Socket s = null;
                            try {
                                s = new Socket(host, port);
                                new Receive(s).start();
                                new Send(s).sendData("type:login&&send:" + LoginForm.username);
                                new HomePage(null, s, LoginForm.username);
                            } catch (IOException e) {
                                System.out.println("Unable to connect to server: " + e.getMessage());
                            }
                            return;
                        }
                        default:
                            System.out.println("Received invalid data: " + receiveMsg);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
