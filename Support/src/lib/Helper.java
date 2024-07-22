package src.lib;

public class Helper {
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
}
