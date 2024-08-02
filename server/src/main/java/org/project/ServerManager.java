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
            new LinkedBlockingQueue<>(LIMIT_QUEUE_SIZE)
        );
    }

    public void startServer(int port) {
        this.PORT = port;
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server listening on port " + port);

                // connect with broker
                Socket brokerSocket = new Socket("localhost", PORT_BROKER);
                System.out.println("Connected to message broker with port " + PORT_BROKER);
                
                BrokerInfo.brokerSocket = brokerSocket;
                new Thread(new Receive(brokerSocket)).start();

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New client connected: " + clientSocket);

                        try {
                            threadPool.submit(new Receive(clientSocket));
                        } catch (RejectedExecutionException e) {
                            System.out.println("Server is overloaded, client will be informed.");
                            Logger.getLogger(ServerManager.class.getName()).log(Level.WARNING, "Server is overloaded, adding client to pending queue. {0}", e.getMessage());
                        }
                        ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;

                        if (tpe.getQueue().remainingCapacity() == 0) {
                            new Send(clientSocket)
                                .sendData("type:error&&data: server is full, please try again later.");
                        }
                    } catch (IOException e) {
                        if (running) {
                            System.out.println("Error accepting connection: " + e.getMessage());
                        }
                        Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error accepting connection: {0}", e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Error starting server: " + e.getMessage());
                Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error starting server: {0}", e.getMessage());

            } finally {
                shutdown();
            }
        }).start();
    }

    public void shutdown() {
        running = false;
        System.out.println("Shutting down server...");
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
                Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error closing server socket: {0}", e.getMessage());
            }
        }
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error shutting down server: {0}", e.getMessage());

                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
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