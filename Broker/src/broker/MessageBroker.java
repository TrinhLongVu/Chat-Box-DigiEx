package broker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CopyOnWriteArrayList;

import src.lib.Send;
import broker.utils.Receive;

public class MessageBroker {
    public static final int PORT = 4000;
    private static final String SERVER_TOPIC = "Server";
    private ConcurrentHashMap<String, List<Socket>> connectedServers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new MessageBroker();
    }

    public MessageBroker() {
        startMessageBroker(PORT);
    }

    public void startMessageBroker(int port) {
        ServerSocket brokerSocket;
        try {
            brokerSocket = new ServerSocket(port);

            // Start server monitor to determine if server is still connected
            new Thread(new ServerMonitor()).start();
            while (true) {
                allowNewServerConnection(brokerSocket);
            }
        } catch (IOException e) {
            Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error starting server: {0}",
                    e.getMessage());
        }
    }

    private void allowNewServerConnection(ServerSocket brokerSocket) {
        try {
            Socket serverSocket = brokerSocket.accept();
            connectedServers.computeIfAbsent(SERVER_TOPIC, k -> new CopyOnWriteArrayList<>()).add(serverSocket);

            new Thread(new ServerHandler(serverSocket)).start();
        } catch (IOException e) {
            Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error accepting connection: {0}",
                    e.getMessage());
        }
    }

    private List<Socket> getSocketsByKey(String key) {
        return connectedServers.getOrDefault(key, new ArrayList<>());
    }

    private class ServerHandler implements Runnable {
        private Socket serverSocket;
        private BufferedReader br;

        @Override
        public void run() {
            try {
                Receive receive = new Receive(serverSocket);
                String message;
                while ((message = br.readLine()) != null) {
                    receive.setReceiveMsg(message);
                    broadcastMessage(message);
                }
            } catch (Exception e) {
                Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error reading from server: {0}",
                        e.getMessage());
            }
        }

        public ServerHandler(Socket serverSocket) {
            this.serverSocket = serverSocket;
            InputStream is;
            try {
                is = this.serverSocket.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
            } catch (IOException e) {
                Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE,
                        "Problem with handling new server: {0}", e.getMessage());
            }
        }

        private void broadcastMessage(String message) {
            for (Socket serverDestination : getSocketsByKey(SERVER_TOPIC)) {
                try {
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Message broadcasted: {0}",
                            message);
                    new Send(serverDestination).sendData(message);
                } catch (IOException e) {
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error broadcasting message: {0}",
                            e.getMessage());
                }
            }
        }
    }

    private class ServerMonitor implements Runnable {
        @Override
        public void run() {
            while (true) {
                Iterator<Socket> iterator = getSocketsByKey(SERVER_TOPIC).iterator();

                while (iterator.hasNext()) {
                    Socket serverSocket = iterator.next();
                    try {
                        serverSocket.getOutputStream().write(0);
                    } catch (IOException e) {
                        removeDisconectedServer(serverSocket);
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public void removeDisconectedServer(Socket serverSocket) {
            try {
                List<Socket> sockets = getSocketsByKey(SERVER_TOPIC);
                if (sockets != null) {
                    sockets.remove(serverSocket);
                    if (sockets.isEmpty()) {
                        connectedServers.remove(SERVER_TOPIC);
                    }
                }
                serverSocket.close();
            } catch (IOException e) {
                Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error closing server socket: {0}",
                        e.getMessage());
            }
        }
    }
}
