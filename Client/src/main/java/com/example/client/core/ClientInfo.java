package com.example.client.core;

import java.io.BufferedReader;

import javax.swing.DefaultListModel;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class ClientInfo {
    private String userName;
    private String severInfo;
    private BufferedReader buffer;
    private DefaultListModel<String> clientList = new DefaultListModel<>();
    private DefaultListModel<String> messageList = new DefaultListModel<>();
}
