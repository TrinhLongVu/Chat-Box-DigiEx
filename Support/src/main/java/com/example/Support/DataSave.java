package com.example.Support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DataSave {
    public static Map<String, LinkedList<String>> contentChat = new HashMap<>();
    public static List<String> userOnline = new ArrayList<>();
    public static String selectedUser = "";
    public static List<Client> clients = new ArrayList<>();
    public static Map<String, String> groups = new HashMap<>();

    private DataSave() {
    }
}
