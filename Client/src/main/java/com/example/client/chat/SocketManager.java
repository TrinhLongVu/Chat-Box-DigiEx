package com.example.client.chat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.net.Socket;

@Getter
@Setter
@Component
public class SocketManager {
    private Socket socket;
}