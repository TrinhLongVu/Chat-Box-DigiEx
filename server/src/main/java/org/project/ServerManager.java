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

import org.project.Chat.Receive;
import src.lib.Send;

public class ServerManager {
    private ServerSocket serverSocket;
    // 1 for thread pool and 1 for client
    private static final int THREAD_POOL_SIZE = 2;
    private static final int LIMIT_QUEUE_SIZE = 1;
    public static int PORT;

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

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New client connected: " + clientSocket);

                        try {
                            Socket brokerSocket = new Socket("localhost", 4000);
                            System.out.println("Connected to message broker");
                
                            new Thread(new Receive(brokerSocket)).start();
                            new Send(brokerSocket).sendData("Hello from server");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            threadPool.submit(new Receive(clientSocket));
                        } catch (RejectedExecutionException e) {
                            System.out.println("Server is overloaded, client will be informed.");
                            Logger.getLogger(ServerManager.class.getName()).log(Level.WARNING, "Server is overloaded, adding client to pending queue. {0}", e.getMessage());
                        }
                        ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
                        System.out.println("Thread pool active count: " + tpe.getActiveCount());
                        System.out.println("Thread pool queued task count: " + tpe.getQueue().size());
                        System.out.println("Thread pool completed task count: " + tpe.getCompletedTaskCount());
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

    public int threadFree() {
        return THREAD_POOL_SIZE - ((ThreadPoolExecutor) threadPool).getActiveCount();
    }
}