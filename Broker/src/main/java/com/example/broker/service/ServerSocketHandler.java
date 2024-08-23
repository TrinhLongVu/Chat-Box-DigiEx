package com.example.broker.service;

import lombok.Getter;
import lombok.Setter;
import java.net.ServerSocket;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
public class ServerSocketHandler {
    private ServerSocket serverSocket;
}
