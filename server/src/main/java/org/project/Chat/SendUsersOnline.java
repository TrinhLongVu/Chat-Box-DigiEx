package org.project.Chat;

import org.project.Services.CallAPI;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import src.lib.Client;
import src.lib.DataSave;
import src.lib.Send;


public class SendUsersOnline {
    public static void handle() {
        CallAPI.GetData("http://localhost:8080/get-clients").thenAccept(userOnline -> {
            if (!userOnline.equals("error")) {
                for (Client client : DataSave.clients) {
                    try {
                        Send sender = new Send(client.getSocket());
                        String user_and_groups[] = userOnline.split("%");
                        String users = "";
                        List<String> listUserInGroups = new ArrayList<>();
                        
                        for (String user_and_group : user_and_groups) {
                            if (user_and_group.contains("group")) {
                                listUserInGroups.add(user_and_group.split(":")[1]);
                            } else {
                                users += user_and_group;
                            }
                        }

                        for (String listUserInGroup : listUserInGroups) {
                            for (String userInGroup : listUserInGroup.split(",")) {
                                System.out.println("user In group...." + userInGroup);
                                if (client.getName().equals(userInGroup)) {
                                    users += "," + listUserInGroup.split(",")[0];
                                }
                            }
                        }
                        sender.sendData("type:online&&data:" + users.replaceAll(",,", ","));
                    } catch (IOException e) {
                        Logger.getLogger(SendUsersOnline.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
                    }
                }
            }
        });
    }
}