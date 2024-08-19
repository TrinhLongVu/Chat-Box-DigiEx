package com.example.Server;

import javax.swing.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Server extends JFrame {
    private JTextField portTextField;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    @Autowired
    private ServerManager serverManager;

    public Server() {
        setTitle("Server Control");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize components
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        statusLabel = new JLabel("Server is stopped.");
        portTextField = new JTextField("1235"); // Default port value

        // Layout setup
        JPanel panel = new JPanel();
        panel.add(new JLabel("Port:"));
        panel.add(portTextField);
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(statusLabel);

        add(panel);

        // Add action listeners
        startButton.addActionListener(e -> {
            if (!serverManager.isRunning()) {
                try {
                    int port = Integer.parseInt(portTextField.getText());
                    serverManager.startServer(port);
                    statusLabel.setText("Server is running on port " + port);
                } catch (NumberFormatException ex) {
                    statusLabel.setText("Invalid port number.");
                }
            } else {
                statusLabel.setText("Server is already running.");
            }
        });

        stopButton.addActionListener(e -> {
            if (serverManager.isRunning()) {
                serverManager.stopServer();
                statusLabel.setText("Server is stopped.");
            } else {
                statusLabel.setText("Server is not running.");
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Server.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}