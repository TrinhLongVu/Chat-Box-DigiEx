package com.example.Server.utils;

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

public class CallAPI {
    private final static String HOST = "localhost";
    private final static String PORT = "8080";
    public static CompletableFuture<String> GetData(String paramString) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("http://" + HOST + ":" + PORT + paramString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "text/plain");

                int responseCode = connection.getResponseCode();
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

    public static void PostData(String paramString, String data) {
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
