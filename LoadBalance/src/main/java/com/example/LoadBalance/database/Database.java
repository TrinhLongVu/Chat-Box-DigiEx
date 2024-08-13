package com.example.LoadBalance.database;

import java.util.ArrayList;
import java.util.List;

import com.example.LoadBalance.payloads.ClientInfo;
import com.example.LoadBalance.payloads.ServerInfo;

public class Database {
    public static List<ClientInfo> clients = new ArrayList<>();
    public static List<ServerInfo> serverList = new ArrayList<>();
}
