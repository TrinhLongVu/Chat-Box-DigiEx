package org.project.Utils;

import org.project.payload.TypeReceive;

public class Helper {
    public static TypeReceive FormatData(String receiveMsg) {

        String[] pairs = receiveMsg.split(",");

        String type = null;
        String send = null;
        String recieve = null;
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
                case "recieve":
                    recieve = value;
                    break;
                case "data":
                    data = value;
                    break;
            }
        }

        TypeReceive result = new TypeReceive(type, send, recieve, data);
        return result;
    }
}
