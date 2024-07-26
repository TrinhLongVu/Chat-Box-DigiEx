package org.project.Chat;
import java.io.IOException;
import src.lib.Client;
import src.lib.DataSave;
import java.util.Map;
import src.lib.Send;
public class SendUsersOnline {
    public static void handle(String userOnlines) {
        String resultSend = "";
        for (Client client : DataSave.clients) {
            resultSend = "type:online&&data:" + getAllExceptMe(userOnlines, client.getName());

            for (Map.Entry<String, String> dataName : DataSave.groups.entrySet()) {
                String[] usersInGroup = dataName.getValue().split(", ");
                if (List.of(usersInGroup).contains(client.getName())) {
                    resultSend = resultSend.substring(0, resultSend.length() - 1) + ", " + dataName.getKey() + "]";
                }
            }

            try {
                new Send(client.getSocket()).sendData(resultSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getAllExceptMe(String listUserOnline, String myName) {
        String regex = "\\b" + myName + "\\b,?\\s?";
        String result = listUserOnline.replaceAll(regex, "");
        result = result.replaceAll(",\\s*\\]", "]");
        result = result.replaceAll("\\[\\s*\\]", "[]");
        return result;
    }
}
