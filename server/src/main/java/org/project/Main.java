package org.project;

import javax.swing.*;

public class Main extends JFrame {
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private ServerManager serverManager;

    public Main() {
        serverManager = new ServerManager();

        setTitle("Server Control");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize components
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        statusLabel = new JLabel("Server is stopped.");

        // Layout setup
        JPanel panel = new JPanel();
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(statusLabel);

        add(panel);

        // Add action listeners
        startButton.addActionListener(e -> {
            if (!serverManager.isRunning()) {
                serverManager.startServer(1235);
                statusLabel.setText("Server is running.");
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
        SwingUtilities.invokeLater(Main::new);
    }
}