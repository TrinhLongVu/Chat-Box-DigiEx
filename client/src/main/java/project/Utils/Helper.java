package project.Utils;

import project.Utils.TypeReceive;
public class Helper {
    public static TypeReceive formatData(String receiveMsg) {
        String[] pairs = receiveMsg.split("&&");

        String type = null;
        String send = null;
        String content = null;

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
                case "content":
                    content = value;
                    break;
                default:
                    break;
            }
        }

        TypeReceive result = new TypeReceive(type, send, content);
        return result;
    }
}
