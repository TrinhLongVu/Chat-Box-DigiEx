package com.example.servers;

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

import com.example.support.Send;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.example.servers.payloads.BrokerInfo;
import com.example.servers.controller.ReceiveController;
import com.example.support.*;

@Component
@AllArgsConstructor
public class ServerManager {
    private static final Logger log = LoggerFactory.getLogger(ServerManager.class);
    private ServerSocket serverSocket;
    private static final int THREAD_POOL_SIZE = 2;
    private static final int LIMIT_QUEUE_SIZE = 1;

    @Value("${app.broker.port}")
    private int BROKER_PORT;

    @Value("${app.broker.host}")
    private String BROKER_HOST;

    @Value("${app.server.host}")
    private String SERVER_HOST;

    @Value("${app.server.port}")
    private int SERVER_PORT;

    @Value("${app.loadbalancer.host}")
    private String LOADBALANCER_HOST;

    @Value("${app.loadbalancer.port}")
    private int LOADBALANCER_PORT;

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

    public void startServer() {
        running = true;

        new Thread(() -> {
            try {
                log.info("Starting new ServerManager on port {}", SERVER_PORT);
                serverSocket = new ServerSocket(SERVER_PORT);
                log.info("Broker: {}", BROKER_PORT);
                Socket brokerSocket = new Socket(BROKER_HOST, BROKER_PORT);

                BrokerInfo.brokerSocket = brokerSocket;
                ReceiveController receive = context.getBean(ReceiveController.class, brokerSocket);
                new Thread(receive).start();

                ThreadPoolExecutor tpe = (ThreadPoolExecutor) threadPool;
                sendServerInfo(SERVER_HOST, SERVER_PORT, tpe.getCorePoolSize());

                while (running) {
                    connectClient(tpe);
                }
            } catch (IOException e) {
                log.error("Error starting server: {}", e.getMessage());
            } finally {
                shutdown();
            }
        }).start();
    }

    private void connectClient(ThreadPoolExecutor tpe) {
        try {
            Socket clientSocket = serverSocket.accept();
            if (tpe.getQueue().remainingCapacity() == 0) {
                new Send(clientSocket)
                        .sendData("type:error&&data: server is full, please try again later.");
            } else {
                submitThreadPool(clientSocket);
            }
        } catch (IOException e) {
            log.error("Error accepting connection: {}", e.getMessage());
        }
    }

    private void submitThreadPool(Socket clientSocket) {
        try {
            ReceiveController receive = context.getBean(ReceiveController.class, clientSocket);
            threadPool.submit(receive);
        } catch (RejectedExecutionException e) {
            log.error("Server is overloaded, adding client to pending queue. {}", e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("Error closing server socket: {}", e.getMessage());
            }
        }
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Error shutting down server: {}", e.getMessage());
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        notifyDisconnection("localhost", SERVER_PORT);
    }

    private void notifyDisconnection(String host, int port) {
        try {
            URL loadBalancerUrl = new URL("http://" + LOADBALANCER_HOST + ":" + LOADBALANCER_PORT + "/server-disconnected");
            HttpURLConnection loadBalancerConn = (HttpURLConnection) loadBalancerUrl.openConnection();
            loadBalancerConn.setRequestMethod("POST");
            loadBalancerConn.setDoOutput(true);
            loadBalancerConn.setRequestProperty("Content-Type", "text/plain");

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
            log.error("Error sending notify server information: {}", e.getMessage());
        }
    }

    public void stopServer() {
        shutdown();
    }

    private void sendServerInfo(String host, int port, int threadSize) {
        try {
            URL loadBalancerUrl = new URL("http://" + LOADBALANCER_HOST + ":" + LOADBALANCER_PORT + "/server-available");

            HttpURLConnection loadBalancerConn = (HttpURLConnection) loadBalancerUrl.openConnection();
            loadBalancerConn.setRequestMethod("POST");
            loadBalancerConn.setDoOutput(true);
            loadBalancerConn.setRequestProperty("Content-Type", "text/plain");

            String confirmationMessage = host + "@" + port + "&&" + threadSize;

            // Add headers
            try (OutputStream os = loadBalancerConn.getOutputStream()) {
                os.write(confirmationMessage.getBytes());
                os.flush();
            } catch (Exception e) {
                log.error("Error OutputStream: {}", e.getMessage());
            }
            log.info("Confirmation Message: {}", confirmationMessage);

            BufferedReader in = new BufferedReader(new InputStreamReader(loadBalancerConn.getInputStream()));
            StringBuilder newContent = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                newContent.append(inputLine);
            }
            in.close();
        } catch (IOException ie) {
            log.error("Error sending server information: {}", ie.getMessage());
        }
    }
}