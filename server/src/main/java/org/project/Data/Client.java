package org.project.Data;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    public static List<Client> clients = new ArrayList<>();
    private String _name;
    private Socket _socket;
    public Client(String name, Socket socket) {
        this._name = name;
        this._socket = socket;
    }

    public String getName() {return this._name;}
    public Socket getSocket() {return this._socket;}
}
