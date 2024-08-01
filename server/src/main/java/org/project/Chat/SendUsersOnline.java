package org.project.Chat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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

class CallAPI {
    public static String GetData(String string_url) {
        try {
            URL url = new URL(string_url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } catch (IOException e) {
            Logger.getLogger(CallAPI.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
        }
        return "error";
    }
}