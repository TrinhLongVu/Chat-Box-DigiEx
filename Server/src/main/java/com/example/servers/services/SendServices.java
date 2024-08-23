package com.example.servers.services;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.example.servers.utils.CallAPI;
import com.example.support.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SendServices {
    private final Logger log = LogManager.getLogger(SendServices.class);

    @Autowired
    private CallAPI callAPI;

    public void SendMessage(Socket sender, String msg) {
        log.info("SendMessage called with sender: {} and message: {}", sender, msg);
        try {
            new Send(sender).sendData(msg);
        } catch (IOException e) {
            log.error("An error occurred: {}", e.getMessage());
        }
    }
    public void SendUserOnline() {
        callAPI.GetData("/get-clients").thenAccept(userOnline -> {
            if (!userOnline.equals("error")) {
                for (Client client : DataSave.clients) {
                    processClient(client, userOnline);
                }
            } else {
               log.error( "Failed to fetch user online data");
            }
        });
    }
    private void processClient(Client client, String userOnline) {
        String[] userAndGroups = userOnline.split("%");
        String users = extractUsers(userAndGroups);
        List<String> listUserInGroups = extractUserGroups(userAndGroups);
        users = appendGroupUsers(client, users, listUserInGroups);
        String sanitizedUsers = users.replaceAll(",,", ",");
        SendMessage(client.getSocket(), "type:online&&data:" + sanitizedUsers);
    }
    private String extractUsers(String[] userAndGroups) {
        StringBuilder users = new StringBuilder();
        for (String userAndGroup : userAndGroups) {
            if (!userAndGroup.contains("group")) {
                users.append(userAndGroup);
            }
        }
        return users.toString();
    }
    private List<String> extractUserGroups(String[] userAndGroups) {
        List<String> listUserInGroups = new ArrayList<>();
        for (String userAndGroup : userAndGroups) {
            if (userAndGroup.contains("group")) {
                listUserInGroups.add(userAndGroup.split(":")[1]);
            }
        }
        return listUserInGroups;
    }
    private String appendGroupUsers(Client client, String users, List<String> listUserInGroups) {
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
