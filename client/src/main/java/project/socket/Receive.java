package project.socket;

import project.Utils.TypeReceive;
import project.Utils.helpers;
import project.View.HomePage;
import project.data.dataChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
                    TypeReceive data = helpers.formatData(receiveMsg);

                    switch (data.getType()){
                        case "online": {
                            String content = data.getContent();
                            String[] namesArray = content.substring(content.indexOf("[") + 1, content.indexOf("]")).split("\\s*,\\s*");
                            List<String> namesList = Arrays.asList(namesArray);
                            dataChat.userOnline = namesList;
                            HomePage.listModelUsers.clear();
                            for(String user : dataChat.userOnline ){
                                HomePage.listModelUsers.addElement(user);
                            }
                            System.out.println(dataChat.selectedUser);
                            break;
                        }
                        case "chat": {
                            String content = data.getContent();
                            String userSend = data.getNameSend();
                            LinkedList history = dataChat.contentChat.get(userSend);
                            if(history == null){
                                history = new LinkedList<>();
                                dataChat.contentChat.put(userSend, history);
                            }
                            history.add(userSend + ": " + content);
                            if(dataChat.selectedUser.equals(userSend)) {
                                HomePage.listModel.clear();
                                for(Object hist: history) {
                                    HomePage.listModel.addElement((String)hist);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
