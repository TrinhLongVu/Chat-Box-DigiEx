package org.project.Services;

import java.net.Socket;
import src.lib.TypeReceive;

public interface InterfaceMessageHandler {
    void handle(TypeReceive data, Socket socket, String message);
}