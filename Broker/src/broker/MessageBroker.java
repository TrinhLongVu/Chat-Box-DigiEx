package broker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import src.lib.Send;
import broker.utils.Receive;

public class MessageBroker {
    public static final int PORT = 4000;
    private ConcurrentHashMap<Socket, Long> connectedServers = new ConcurrentHashMap<>();
    private static final int HEARTBEAT_TIMEOUT = 15000;

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

            // Start heartbeat monitoring thread
            new Thread(new HeartbeatMonitor()).start();

            while (true) {
                try {
                    Socket serverSocket = brokerSocket.accept();
                    connectedServers.put(serverSocket, System.currentTimeMillis());

                    new Thread(new ServerHandler(serverSocket)).start();
                } catch (IOException e) {
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error accepting connection: {0}", e.getMessage());
                }
            }
        } catch (IOException e) {
            Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error starting server: {0}", e.getMessage());
        }
    }

    private class ServerHandler implements Runnable {
        private Socket serverSocket;
        private BufferedReader br;
        
        public ServerHandler(Socket serverSocket) {
            this.serverSocket = serverSocket;
            InputStream is;
            try {
                is = this.serverSocket.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));
            } catch (IOException e) {
                Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Problem with handling new server: {0}", e.getMessage());
            }
        }

        private void broadcastMessage(String message, Socket senderSocket) {
            for (Socket serverDestination : connectedServers.keySet()) {
                try {
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.INFO, "Message broadcasted: {0}", message);
                    new Send(serverDestination).sendData(message);
                } catch (IOException e) {
                    Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error broadcasting message: {0}", e.getMessage());
                }
            }
        }

        @Override
        public void run() {
            try {
                Receive receive = new Receive(serverSocket);
                String message;
                while ((message = br.readLine()) != null) {
                    receive.setReceiveMsg(message);

                    if (message.equals("type:heartbeat")) {
                        connectedServers.put(serverSocket, System.currentTimeMillis());
                    } else {
                        broadcastMessage(message, serverSocket);
                    }
                }
            } catch (Exception e) {
                Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error reading from server: {0}", e.getMessage());
            }
        }
    }

    

    private class HeartbeatMonitor implements Runnable {
        @Override
        public void run() {
            while (true) {
                long currentTime = System.currentTimeMillis();
                Iterator<Socket> iterator = connectedServers.keySet().iterator();

                while (iterator.hasNext()) {
                    Socket serverSocket = iterator.next();
                    long lastHeartbeat = connectedServers.get(serverSocket);

                    if (currentTime - lastHeartbeat > HEARTBEAT_TIMEOUT) {
                        iterator.remove();
                        try {
                            serverSocket.close();
                        } catch (IOException e) {
                            Logger.getLogger(MessageBroker.class.getName()).log(Level.SEVERE, "Error closing server socket: {0}", e.getMessage());
                        }
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
    }
}
