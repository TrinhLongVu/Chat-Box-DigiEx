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
        String userOnline = CallAPI.GetData("http://localhost:8080/get-clients");
        for (Client client : DataSave.clients) {
            try {
                new Send(client.getSocket()).sendData("type:online&&data:" + userOnline + "," + newUser);
            } catch (IOException e) {
                Logger.getLogger(SendUsersOnline.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
            }
        }
    }
}