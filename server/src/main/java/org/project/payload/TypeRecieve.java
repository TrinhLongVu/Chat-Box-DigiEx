package org.project.payload;

// "type: login, name: Hoang"
public class TypeRecieve {
    private String _type;
    private String _send;
    private String _recieve;
    private String _data;


    public TypeRecieve(String type, String send, String recieve, String data) {
        _type = type;
        _recieve = recieve;
        _data = data;
        _send = send;
    }

    public String getType() {return _type;}
    public String getIdSend() {return _send;}
    public String getIdRecieve() {return _recieve;}
    public String getData() {return _data;}
}
