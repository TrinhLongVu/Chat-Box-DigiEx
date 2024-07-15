package org.project.Utils;

import org.project.payload.TypeRecieve;

public class helper {
    public static TypeRecieve FormatData(String receiveMsg) {

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

        TypeRecieve result = new TypeRecieve(type, send, recieve, data);
        return result;
    }
}
