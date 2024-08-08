package org.project.Services;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.project.Utils.CallAPI;

import src.lib.Client;
import src.lib.DataSave;
import src.lib.Send;

public class SendServices {
    private static final Logger LOGGER = Logger.getLogger(SendServices.class.getName());

    public static void SendMessage(Socket sender, String msg) {
        LOGGER.log(Level.INFO, "SendMessage called with sender: {0} and message: {1}", new Object[] { sender, msg });
        try {
            new Send(sender).sendData(msg);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
        }
    }
    
    public static void SendUserOnline() {
        CallAPI.GetData("/get-clients").thenAccept(userOnline -> {
            if (!userOnline.equals("error")) {
                for (Client client : DataSave.clients) {
                    processClient(client, userOnline);
                }
            } else {
                LOGGER.log(Level.SEVERE, "Failed to fetch user online data");
            }
        });
    }

    private static void processClient(Client client, String userOnline) {
        String[] userAndGroups = userOnline.split("%");
        String users = extractUsers(userAndGroups);
        List<String> listUserInGroups = extractUserGroups(userAndGroups);
        users = appendGroupUsers(client, users, listUserInGroups);

        String sanitizedUsers = users.replaceAll(",,", ",");
        SendServices.SendMessage(client.getSocket(), "type:online&&data:" + sanitizedUsers);
    }

    private static String extractUsers(String[] userAndGroups) {
        StringBuilder users = new StringBuilder();
        for (String userAndGroup : userAndGroups) {
            if (!userAndGroup.contains("group")) {
                users.append(userAndGroup);
            }
        }
        return users.toString();
    }

    private static List<String> extractUserGroups(String[] userAndGroups) {
        List<String> listUserInGroups = new ArrayList<>();
        for (String userAndGroup : userAndGroups) {
            if (userAndGroup.contains("group")) {
                listUserInGroups.add(userAndGroup.split(":")[1]);
            }
        }
        return listUserInGroups;
    }

    private static String appendGroupUsers(Client client, String users, List<String> listUserInGroups) {
        StringBuilder usersBuilder = new StringBuilder(users);
        for (String listUserInGroup : listUserInGroups) {
            String[] usersInGroup = listUserInGroup.split(",");
            String groupName = usersInGroup[0];
            for (String userInGroup : usersInGroup) {
                if (client.getName().equals(userInGroup)) {
                    usersBuilder.append(",").append(groupName);
                }
            }
        }
        return usersBuilder.toString();
    }
}
