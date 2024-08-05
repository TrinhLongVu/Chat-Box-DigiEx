package project;

import java.io.IOException;
import java.net.Socket;

import project.Chat.ServerInfo;

public class Utils {
    public static boolean isServerRunning(ServerInfo server) {
        try (Socket socket = new Socket(server.getHost(), server.getPort())) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
