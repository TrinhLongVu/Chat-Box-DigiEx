package com.example.client.view;

import javax.swing.*;

import com.example.client.chat.MessageManager;
import com.example.client.chat.SocketManager;
import com.example.client.core.ClientInfo;
import com.example.client.utils.LoadBalanceManager;
import com.example.support.Helper;
import com.example.support.Send;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.Socket;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginForm extends JDialog {
    private static final Logger log = LogManager.getLogger(LoginForm.class);
    private final LoadBalanceManager loadBalanceManager;
    private final MessageManager messageManager;
    private final SocketManager socketManager;
    private final ClientInfo clientInfo;
    private final HomePage homePage;
    private JTextField tfEmail;

    public void init() {
        setTitle("Login");
        setMinimumSize(new Dimension(450, 150));
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel loginPanel = new JPanel(new GridLayout(2, 2));
        tfEmail = new JTextField(20);
        JButton btnOK = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");

        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(tfEmail);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        getContentPane().add(loginPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        btnOK.addActionListener(_ -> {
            if (tfEmail.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a username.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                clientInfo.setUserName(tfEmail.getText());
                if (!handleServer().equals("null")) {
                    dispose();
                }
            }
        });

        btnCancel.addActionListener(e -> {
            dispose();
            System.exit(0);
        });

        setVisible(true);
    }

    private String handleServer() {
        String connectString = loadBalanceManager.getConnectResponse();
        String data = Helper.formatData(connectString).getData();

        if (data.equals("null")) {
            JOptionPane.showMessageDialog(null, "Don't have any available servers.", "Notification", JOptionPane.INFORMATION_MESSAGE);
            return "null";
        }

        String[] hostAndPort = data.split("@");
        int port;
        String host = "localhost";

        try {
            port = Integer.parseInt(hostAndPort[1]);
        } catch (NumberFormatException e) {
            log.error("Invalid port number format: {}", e.getMessage());
            return "null";
        }

        try {
            Socket s = new Socket(host, port);
            if (s.isConnected()) {
                socketManager.setSocket(s);
                loadBalanceManager.notifyConnected(host, port, clientInfo.getUserName());
            }

            new Send(s).sendData("type:login&&send:" + clientInfo.getUserName());

            socketManager.initializeBufferedReader(s);
            homePage.setName(clientInfo.getUserName());
            homePage.init();
        } catch (IOException e) {
            log.error("Unable to connect to server: {}", e.getMessage());
        }

        return "login";
    }
}