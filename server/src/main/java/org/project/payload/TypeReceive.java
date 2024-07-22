package org.project.Payload;

// "type: login, name: Hoang"
public class TypeReceive {
    private String _type;
    private String _send;
    private String _receive;
    private String _data;


    public TypeReceive(String type, String send, String receive, String data) {
        _type = type;
        _receive = receive;
        _data = data;
        _send = send;
    }

    public String getType() {return _type;}
    public String getNameSend() {return _send;}
    public String getNameReceive() {return _receive;}
    public String getData() {return _data;}
}
