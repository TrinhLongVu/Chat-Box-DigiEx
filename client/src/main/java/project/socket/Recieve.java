package project.socket;

import project.Utils.TypeRecieve;
import project.Utils.helpers;
import project.View.HomePage;
import project.data.dataChat;

import javax.swing.JList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Recieve extends Thread {
    String receiveMsg = "";
    BufferedReader br;

    public Recieve(Socket ss) {
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
                    TypeRecieve data = helpers.formatData(receiveMsg);

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
                            if(HomePage.selectedUser.equals(userSend)) {
                                HomePage.listModel.clear();
                                for(Object hist: history) {
                                    HomePage.listModel.addElement((String)hist);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
