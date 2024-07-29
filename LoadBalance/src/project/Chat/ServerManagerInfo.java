package project.Chat;

import org.project.ServerManager;

public class ServerManagerInfo {
    private ServerManager server;
    private int port;
    private boolean isOpenning = true;

    public ServerManagerInfo(ServerManager server, int port) {
        this.server = server;
        this.port = port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return this.port;
    }

    public ServerManager getServerManager() {
        return this.server;
    }

    public void setServerManager(ServerManager server) {
        this.server = server;
    }

    public void setOpenning(boolean isOpenning) {
        this.isOpenning = isOpenning;
    }

    public boolean getOpenning() {
        return isOpenning;
    }
}
