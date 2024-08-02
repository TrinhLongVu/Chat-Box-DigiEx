package org.project.Chat;

import org.project.Services.CallAPI;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import src.lib.Client;
import src.lib.DataSave;
import src.lib.Send;


public class SendUsersOnline {
    public static void handle(String newUser) {
        CallAPI.GetData("http://localhost:8080/get-clients").thenAccept(userOnline -> {
            if (!userOnline.equals("error")) {
                for (Client client : DataSave.clients) {
                    System.out.println(client.getName() + "...." + userOnline);
                    try {
                        Send sender = new Send(client.getSocket());
                        if (newUser != null) {
                            sender.sendData("type:online&&data:" + userOnline + "," + newUser);
                        } else {
                            sender.sendData("type:online&&data:" + userOnline);
                        }
                    } catch (IOException e) {
                        Logger.getLogger(SendUsersOnline.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
                    }
                }
            }
        });
    }
}