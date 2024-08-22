package com.example.client.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.io.BufferedReader;

@Getter
@Setter
@Component
public class ClientInfo {
    private String userName;
    private BufferedReader buffer;
    private DefaultListModel<String> clientList = new DefaultListModel<>();
    private DefaultListModel<String> messageList = new DefaultListModel<>();
}
