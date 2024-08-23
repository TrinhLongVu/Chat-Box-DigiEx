package com.example.servers.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class CallAPI {
    @Value("${app.loadbalancer.host}")
    private String HOST;
    @Value("${app.loadbalancer.port}")
    private String PORT;

    public CompletableFuture<String> GetData(String paramString) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("http://" + HOST + ":" + PORT + paramString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "text/plain");

                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } catch (IOException e) {
                Logger.getLogger(CallAPI.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
            }
            return "error";
        });
    }

    public void PostData(String paramString, String data) {
        try {
            URL url = new URL("http://" + HOST + ":" + PORT + paramString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain");

            // Write the data
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
        } catch (IOException e) {
            Logger.getLogger(CallAPI.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
        }
    }
}