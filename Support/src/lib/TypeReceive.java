package src.lib;

public class TypeReceive {
    private String type;
    private String send;
    private String receive;
    private String data;


    public TypeReceive(String type, String send, String receive, String data) {
        this.type = type;
        this.receive = receive;
        this.data = data;
        this.send = send;
    }

    public String getType() {return this.type;}
    public String getNameSend() {return this.send;}
    public String getNameReceive() {return this.receive;}
    public String getData() {return this.data;}
}
