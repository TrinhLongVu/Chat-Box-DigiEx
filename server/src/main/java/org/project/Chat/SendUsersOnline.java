package org.project.Chat;
import java.io.IOException;
import src.lib.Client;
import src.lib.DataSave;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import src.lib.Send;

public class SendUsersOnline {
    public static void handle(String newUser) {
        for (Client client : DataSave.clients) {
            try {
                if(!client.getName().equals(newUser))
                    new Send(client.getSocket()).sendData("type:online&&data:" + newUser);
            } catch (IOException e) {
                Logger.getLogger(SendUsersOnline.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
            }
        }
    }
}
