package org.project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.project.Chat.BrokerInfo;
import org.project.Chat.Receive;
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
                Logger.getLogger(ServerManager.class.getName()).log(Level.INFO, "Starting new ServerManager: {0}", String.valueOf(port));
                serverSocket = new ServerSocket(port);

                // connect with broker
                Socket brokerSocket = new Socket("localhost", PORT_BROKER);

                BrokerInfo.brokerSocket = brokerSocket;
                new Thread(new Receive(brokerSocket)).start();

                new Thread(new HeartbeatSender(brokerSocket)).start();
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();

                        ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
                        if (tpe.getQueue().remainingCapacity() == 0) {
                            new Send(clientSocket)
                                    .sendData("type:error&&data: server is full, please try again later.");
                        } else {
                            try {
                                threadPool.submit(new Receive(clientSocket));
                            } catch (RejectedExecutionException e) {
                                Logger.getLogger(ServerManager.class.getName()).log(Level.WARNING,
                                        "Server is overloaded, adding client to pending queue. {0}", e.getMessage());
                            }
                        }
                    } catch (IOException e) {
                        Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE,
                                "Error accepting connection: {0}", e.getMessage());
                    }
                }
            } catch (IOException e) {
                Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error starting server: {0}",
                        e.getMessage());

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
}