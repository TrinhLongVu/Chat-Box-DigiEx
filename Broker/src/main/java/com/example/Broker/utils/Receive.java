package com.example.Broker.utils;

import java.net.Socket;

public class Receive {
    private String receiveMsg;
    private Socket socket;

    public Receive(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public String receiveMessage() {
        return receiveMsg;
    }

    public void setReceiveMsg(String receiveMsg) {
        this.receiveMsg = receiveMsg;
    }
}

