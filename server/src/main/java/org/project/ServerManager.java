package org.project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.project.Chat.Receive;
import src.lib.Send;

public class ServerManager {
    private ServerSocket serverSocket;
    private static final int THREAD_POOL_SIZE = 3;
    private static final int LIMIT_QUEUE_SIZE = 1;

    private volatile boolean running;
    private ExecutorService threadPool;
    private LinkedBlockingQueue<Socket> pendingConnections;

    public ServerManager() {
        threadPool = new ThreadPoolExecutor(
            THREAD_POOL_SIZE,
            THREAD_POOL_SIZE,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(LIMIT_QUEUE_SIZE)
        );
        pendingConnections = new LinkedBlockingQueue<>();
    }

    public void startServer(int port) {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server listening on port " + port);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New client connected: " + clientSocket);
                        
                        if (threadFree() > 2) {
                            System.out.println("waiting.....");
                            // new Send(clientSocket).sendData("Server is overloaded, adding client to pending queue.");
                        }
                        
                        try {
                            threadPool.submit(new Receive(clientSocket));
                        } catch (RejectedExecutionException e) {
                            System.out.println("Server is overloaded, adding client to pending queue.");
                         //   new Send(clientSocket).sendData("Server is overloaded, adding client to pending queue.");
                            // handle overloaded
                            pendingConnections.offer(clientSocket);
                        }
                        System.out.println("quantity thread ::::: " + Thread.activeCount());
                        System.out.println("pool thread ::::: " + ((ThreadPoolExecutor) threadPool).getActiveCount());
                        System.out.println("Queued task count: " + ((ThreadPoolExecutor) threadPool).getQueue().size());

                    } catch (IOException e) {
                        if (running) {
                            System.out.println("Error accepting connection: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Error starting server: " + e.getMessage());
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
            }
        }
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
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