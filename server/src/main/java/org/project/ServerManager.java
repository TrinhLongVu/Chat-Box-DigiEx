package org.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.project.Payloads.BrokerInfo;
import org.project.Controller.ReceiveController;

import src.lib.Send;

public class ServerManager {
    private ServerSocket serverSocket;

    private static final int THREAD_POOL_SIZE = 2;
    private static final int LIMIT_QUEUE_SIZE = 1;
    public static int PORT;
    private final int PORT_BROKER = 4000;

    private volatile boolean running;
    private ExecutorService threadPool;

    public ServerManager() {
        threadPool = new ThreadPoolExecutor(
                THREAD_POOL_SIZE,
                THREAD_POOL_SIZE,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(LIMIT_QUEUE_SIZE));
    }

    public void startServer(int port) {
        this.PORT = port;
        running = true;

        new Thread(() -> {
            try {
                Logger.getLogger(ServerManager.class.getName()).log(Level.INFO, "Starting new ServerManager on port {0}", port);
                serverSocket = new ServerSocket(port);

                // connect with broker
                Socket brokerSocket = new Socket("localhost", PORT_BROKER);

                BrokerInfo.brokerSocket = brokerSocket;
                new Thread(new ReceiveController(brokerSocket)).start();
                new Thread(new HeartbeatSender(brokerSocket)).start();
                ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
                sendServerInfo("localhost", port, tpe.getCorePoolSize());

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        if (tpe.getQueue().remainingCapacity() == 0) {
                            new Send(clientSocket).sendData("type:error&&data: server is full, please try again later.");
                        } else {
                            try {
                                threadPool.submit(new ReceiveController(clientSocket));
                            } catch (RejectedExecutionException e) {
                                Logger.getLogger(ServerManager.class.getName()).log(Level.WARNING, "Server is overloaded, adding client to pending queue. {0}", e.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error accepting connection: {0}", e.getMessage());
                    }
                }
            } catch (IOException e) {
                Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error starting server: {0}", e.getMessage());
            } finally {
                shutdown();
            }
        }).start();
    }


    public void shutdown() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error closing server socket: {0}",
                        e.getMessage());
            }
        }
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error shutting down server: {0}",
                        e.getMessage());

                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        notifyDisconnection("localhost", PORT);
    }
    
    private void notifyDisconnection(String host, int port) {
        try {
            URL loadBalancerUrl = new URL("http://localhost:8080/server-disconnected");

            HttpURLConnection loadBalancerConn = (HttpURLConnection) loadBalancerUrl.openConnection();
            loadBalancerConn.setRequestMethod("POST");
            loadBalancerConn.setDoOutput(true);

            String confirmationMessage = host + "@" + port;
            try (OutputStream os = loadBalancerConn.getOutputStream()) {
                os.write(confirmationMessage.getBytes());
                os.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(loadBalancerConn.getInputStream()));
            StringBuilder newContent = new StringBuilder();

            String inputLine;
            
            while ((inputLine = in.readLine()) != null) {
                newContent.append(inputLine);
            }

            // Close connections
            in.close();
            loadBalancerConn.disconnect();
        } catch (IOException e) {
            Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error sending server information: {0}",
                    e.getMessage());
        }
    }

    private class HeartbeatSender implements Runnable {
        private Socket brokerSocket;
        private static final int HEARTBEAT_INTERVAL = 3;

        public HeartbeatSender(Socket brokerSocket) {
            this.brokerSocket = brokerSocket;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    new Send(brokerSocket).sendData("type:heartbeat");
                    TimeUnit.SECONDS.sleep(HEARTBEAT_INTERVAL);
                } catch (IOException e) {
                    System.err.println("Error sending heartbeat: " + e.getMessage());
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Heartbeat sender interrupted: " + e.getMessage());
                    break;
                }
            }
        }
    }

    public void stopServer() {
        shutdown();
    }

    public boolean isRunning() {
        return running;
    }

    private void sendServerInfo(String host, int port, int threadSize) {
        try {
            URL loadBalancerUrl = new URL("http://localhost:8080/server-available");

            HttpURLConnection loadBalancerConn = (HttpURLConnection) loadBalancerUrl.openConnection();
            loadBalancerConn.setRequestMethod("POST");
            loadBalancerConn.setDoOutput(true);

            String confirmationMessage = host + "@"+ port + "&&" + threadSize;
            try (OutputStream os = loadBalancerConn.getOutputStream()) {
                os.write(confirmationMessage.getBytes());
                os.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(loadBalancerConn.getInputStream()));
            StringBuilder newContent = new StringBuilder();

            String inputLine;
            
            while ((inputLine = in.readLine()) != null) {
                newContent.append(inputLine);
            }

            // Close connections
            in.close();
            loadBalancerConn.disconnect();
        } catch (IOException e) {
            Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error sending server information: {0}",
                    e.getMessage());
        }
    }
}