package com.example.Server;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.example.Server.payloads.BrokerInfo;
import com.example.Server.controller.ReceiveController;
import com.example.Support.*;

@Component
public class ServerManager {
    public static int PORT;
    private ServerSocket serverSocket;
    private static final int THREAD_POOL_SIZE = 2;
    private static final int LIMIT_QUEUE_SIZE = 1;
    private final int PORT_BROKER = 4000;
    private volatile boolean running;
    private ExecutorService threadPool;

    @Autowired
    private ApplicationContext context; 

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
                Logger.getLogger(ServerManager.class.getName()).log(Level.INFO,
                        "Starting new ServerManager on port {0}", port);
                serverSocket = new ServerSocket(port);
                Socket brokerSocket = new Socket("localhost", PORT_BROKER);

                BrokerInfo.brokerSocket = brokerSocket;
                ReceiveController receive = context.getBean(ReceiveController.class, brokerSocket);
                new Thread(receive).start();

                ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
                sendServerInfo("localhost", port, tpe.getCorePoolSize());

                while (running) {
                    ConnectClient(tpe);
                }
            } catch (IOException e) {
                Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error starting server: {0}",
                        e.getMessage());
            } finally {
                shutdown();
            }
        }).start();
    }

    private void ConnectClient(ThreadPoolExecutor tpe) {
        try {
            Socket clientSocket = serverSocket.accept();
            if (tpe.getQueue().remainingCapacity() == 0) {
                new Send(clientSocket)
                        .sendData("type:error&&data: server is full, please try again later.");
            } else {
                SubmitThreadPool(clientSocket);
            }
        } catch (IOException e) {
            Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE,
                    "Error accepting connection: {0}", e.getMessage());
        }
    }
    
    private void SubmitThreadPool(Socket clientSocket) {
        try {
            ReceiveController receive = context.getBean(ReceiveController.class, clientSocket);
            threadPool.submit(receive);
        } catch (RejectedExecutionException e) {
            Logger.getLogger(ServerManager.class.getName()).log(Level.WARNING,
                    "Server is overloaded, adding client to pending queue. {0}", e.getMessage());
        }
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
    
    private void setMethodHttp(HttpURLConnection loadBalancerConn) {
        loadBalancerConn.setRequestMethod("POST");
        loadBalancerConn.setDoOutput(true);
        loadBalancerConn.setRequestProperty("Content-Type", "text/plain");
    }

    private void notifyDisconnection(String host, int port) {
        try {
            URL loadBalancerUrl = new URL("http://localhost:8080/server-disconnected");
            HttpURLConnection loadBalancerConn = (HttpURLConnection) loadBalancerUrl.openConnection();
            setMethodHttp(loadBalancerConn);

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
            in.close();
        } catch (IOException e) {
            Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error sending server information: {0}", e.getMessage());
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
            loadBalancerConn.setRequestProperty("Content-Type", "text/plain");

            String confirmationMessage = host + "@" + port + "&&" + threadSize;
            // Add headers
            try (OutputStream os = loadBalancerConn.getOutputStream()) {
                os.write(confirmationMessage.getBytes());
                os.flush();
            }
            System.out.println("confirmationMessage: " + confirmationMessage);

            BufferedReader in = new BufferedReader(new InputStreamReader(loadBalancerConn.getInputStream()));
            StringBuilder newContent = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                newContent.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            Logger.getLogger(ServerManager.class.getName()).log(Level.SEVERE, "Error sending server information: {0}",
                    e.getMessage());
        }
    }
}
