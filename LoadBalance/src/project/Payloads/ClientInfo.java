package project.Payloads;

public class ClientInfo {
    private String name;
    private String serverinfo;

    public ClientInfo(String name, String serverinfo) {
        this.name = name;
        this.serverinfo = serverinfo;
    }

    public String getName() {
        return name;
    }

    public String getServerinfo() {
        return serverinfo;
    }
}