package com.example.servers.services;

import com.example.support.*;
import java.net.Socket;

public interface InterfaceMessageHandler {
    void handle(TypeReceive data, Socket socket, String message);
}