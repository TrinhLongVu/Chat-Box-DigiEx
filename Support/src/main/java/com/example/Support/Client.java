package com.example.Support;

import java.net.Socket;

public class Client {
    private String name;
    private Socket socket;
    public Client(String name, Socket socket) {
        this.name = name;
        this.socket = socket;
    }

    public String getName() {return this.name;}
    public Socket getSocket() {return this.socket;}
}
