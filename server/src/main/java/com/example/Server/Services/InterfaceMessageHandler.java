package com.example.Server.services;

import com.example.Support.*;
import java.net.Socket;

public interface InterfaceMessageHandler {
    void handle(TypeReceive data, Socket socket, String message);
}