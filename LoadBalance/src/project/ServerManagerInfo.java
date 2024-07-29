package project;

import org.project.ServerManager;

public class ServerManagerInfo {
    Integer port;
    ServerManager serverManager;
    boolean isRunning;

    public ServerManagerInfo(Integer port, ServerManager serverManager) {
        this.port = port;
        this.serverManager = serverManager;
    }

    public Integer getPort() {
        return port;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public boolean getIsRunning() {
        return isRunning;
    }
}