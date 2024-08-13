package com.example.Support.lib;

import java.util.HashMap;
import java.util.Map;

public class Helper {
    public static TypeReceive FormatData(String receiveMsg) {
        String[] pairs = receiveMsg.split("&&");

        Map<String, String> keyValueMap = new HashMap<>();

        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                keyValueMap.put(key, value);
            }
        }

        // Retrieve values from the map
        String type = keyValueMap.getOrDefault("type", null);
        String send = keyValueMap.getOrDefault("send", null);
        String receive = keyValueMap.getOrDefault("receive", null);
        String data = keyValueMap.getOrDefault("data", null);
        String flag = keyValueMap.getOrDefault("flag", null);

        return new TypeReceive(type, send, receive, data, flag);
    }
}
