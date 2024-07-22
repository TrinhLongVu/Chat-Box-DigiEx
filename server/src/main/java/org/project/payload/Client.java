package org.project.Payload;

import java.net.Socket;


public class Client {
    private String _name;
    private Socket _socket;
    public Client(String name, Socket socket) {
        this._name = name;
        this._socket = socket;
    }

    public String getName() {return this._name;}
    public Socket getSocket() {return this._socket;}
}
