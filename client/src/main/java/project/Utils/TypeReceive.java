package project.Utils;

// "type: login, name: Hoang"
public class TypeReceive {
    private String _type;
    private String _send;
    private String _content;


    public TypeReceive(String type, String send, String content) {
        _type = type;
        _content = content;
        _send = send;
    }

    public String getType() {return _type;}
    public String getNameSend() {return _send;}
    public String getContent() {return _content;}
}
