package org.project.Utils;

import org.project.Chat.Send;
import org.project.Data.Client;
import org.project.Data.DataSave;
import org.project.payload.TypeReceive;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class helper {
    public static TypeReceive FormatData(String receiveMsg) {

        String[] pairs = receiveMsg.split("&&");

        String type = null;
        String send = null;
        String receive = null;
        String data = null;

        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            switch (key) {
                case "type":
                    type = value;
                    break;
                case "send":
                    send = value;
                    break;
                case "receive":
                    receive = value;
                    break;
                case "data":
                    data = value;
                    break;
                default:
                    break;
            }
        }

        TypeReceive result = new TypeReceive(type, send, receive, data);
        return result;
    }

    public static void sendUserOnline() {
        String resultSend = "";
        for(Client client: DataSave.clients) {
            List<String> names = DataSave.clients.stream()
                    .filter((c) -> !c.getName().equals(client.getName()))
                    .map((clientName) -> clientName.getName())
                    .collect(Collectors.toList());
            resultSend = "type:online&&content:" + names.toString();

            for (Map.Entry<String, String> dataName : DataSave.groups.entrySet())  {
                String usersInGroup[] = dataName.getValue().split(", ");
                for(String userInGroup: usersInGroup) {
                    if(userInGroup.equals(client.getName())) {
                        resultSend = resultSend.substring(0, resultSend.length() - 1) + ", " + dataName.getKey() + "]";
                    }
                }
            }

            new Send(client.getSocket()).sendData(resultSend);
        }
    }
}
