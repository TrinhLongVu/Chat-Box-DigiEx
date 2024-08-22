package com.example.loadbalance.database;

import java.util.ArrayList;
import java.util.List;

import com.example.loadbalance.payloads.ClientInfo;
import com.example.loadbalance.payloads.ServerInfo;

public class Database {
    public static List<ClientInfo> clients = new ArrayList<>();
    public static List<ServerInfo> serverList = new ArrayList<>();
}
