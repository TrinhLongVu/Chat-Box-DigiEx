package com.example.Support.lib;

public class TypeReceive {
    private String type;
    private String send;
    private String receive;
    private String data;
    private String flag;


    public TypeReceive(String type, String send, String receive, String data, String flag) {
        this.type = type;
        this.receive = receive;
        this.data = data;
        this.send = send;
        this.flag = flag;
    }

    public String getType() {return this.type;}
    public String getNameSend() {return this.send;}
    public String getNameReceive() {return this.receive;}
    public String getData() {return this.data;}

    public boolean isSendBroker() {
        return flag != null ? true : false; 
    }
}
