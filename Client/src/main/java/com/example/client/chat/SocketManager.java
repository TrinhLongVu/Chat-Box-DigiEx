package com.example.client.chat;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

@Getter
@Setter
@Component
public class SocketManager {
    private static final Logger log = LoggerFactory.getLogger(SocketManager.class);
    private BufferedReader buffer;
    private Socket socket;

    public void initializeBufferedReader(Socket connSocket) {
        try {
            InputStream is = connSocket.getInputStream();
            buffer = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            log.error("An error occurred while setting buffer: {} ", e.getMessage());
        }
    }
}