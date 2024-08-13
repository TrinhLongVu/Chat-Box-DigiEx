package com.example.Client.chat;

import java.net.Socket;

public class SocketManager {
    private static Socket socket;

    private SocketManager() {
    }

    public static Socket getSocket() {
        return socket;
    }

    public static void setSocket(Socket newSocket) {
        socket = newSocket;
    }
}
