package org.project.Chat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.project.Services.CallAPI;

import src.lib.Client;
import src.lib.DataSave;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;
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