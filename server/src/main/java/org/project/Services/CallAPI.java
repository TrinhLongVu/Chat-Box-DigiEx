package org.project.Services;

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
    public static CompletableFuture<String> GetData(String string_url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(string_url);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.out.println("response:::" + response.toString());
                    return response.toString();
                }
            } catch (IOException e) {
                Logger.getLogger(CallAPI.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
            }
            return "error";
        });
    }

    public static void PostData(String string_url, String data) {
        try {
            URL url = new URL(string_url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

            // Write the data
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read the response
            int responseCode = connection.getResponseCode();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println(response.toString());
            }
        } catch (IOException e) {
            Logger.getLogger(CallAPI.class.getName()).log(Level.SEVERE, "An error occurred: {0}", e.getMessage());
        }
    }
}
