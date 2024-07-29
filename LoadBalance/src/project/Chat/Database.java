package project.Chat;

import java.util.concurrent.CopyOnWriteArrayList;

import project.ServerManagerInfo;

import org.project.ServerManager;

public class Database {
    public static CopyOnWriteArrayList<ClientInfo> clients = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<ServerInfo> serverList = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<ServerManagerInfo> serverManagerInfoList = new CopyOnWriteArrayList<>();
}

