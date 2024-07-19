package project.socket;

import project.Utils.TypeReceive;
import project.View.HomePage;
import project.data.DataSave;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import project.Utils.Helper;
public class Receive extends Thread {
    String receiveMsg = "";
    BufferedReader br;

    public Receive(Socket ss) {
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
                    TypeReceive data = Helper.formatData(receiveMsg);

                    switch (data.getType()){
                        case "online": {
                            String content = data.getContent();
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
                            String content = data.getContent();
                            String userSend = data.getNameSend();
                            LinkedList history = project.data.DataSave.contentChat.get(userSend);
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
                            String content = data.getContent();
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
