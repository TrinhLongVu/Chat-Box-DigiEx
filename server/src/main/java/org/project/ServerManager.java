package org.project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.project.Chat.Receive;
import src.lib.Send;

public class ServerManager {
    private ServerSocket serverSocket;
    // 1 for thread pool and 1 for client
    private static final int THREAD_POOL_SIZE = 2;
    private static final int LIMIT_QUEUE_SIZE = 1;

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
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server listening on port " + port);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New client connected: " + clientSocket);
                        boolean clientHandled = false;
                        try {
                            threadPool.submit(new Receive(clientSocket));
                            clientHandled = true;
                        } catch (RejectedExecutionException e) {
                            System.out.println("Server is overloaded, client will be informed.");
                            Logger.getLogger(ServerManager.class.getName()).log(null, "Server is overloaded, adding client to pending queue. " + e.getMessage());
                        }
                        ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
                        System.out.println("Thread pool active count: " + tpe.getActiveCount());
                        System.out.println("Thread pool queued task count: " + tpe.getQueue().size());
                        System.out.println("Thread pool completed task count: " + tpe.getCompletedTaskCount());
                        System.out.println("send" + clientHandled + "...");
                        if (tpe.getQueue().remainingCapacity() == 0) {
                            new Send(clientSocket)
                                .sendData("type:error&&data: server is full, please try again later.");
                        }
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