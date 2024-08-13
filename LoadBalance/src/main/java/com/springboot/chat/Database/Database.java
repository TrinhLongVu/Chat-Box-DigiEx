package com.springboot.chat.Database;
import java.util.ArrayList;
import java.util.List;

import com.springboot.chat.Payloads.ClientInfo;
import com.springboot.chat.Payloads.ServerInfo;

public class Database {
    public static List<ClientInfo> clients = new ArrayList<>();
    public static List<ServerInfo> serverList = new ArrayList<>();
}
