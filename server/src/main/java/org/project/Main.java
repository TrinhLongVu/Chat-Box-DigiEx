package org.project;

import javax.swing.*;

public class Main extends JFrame {

    private int SERVER_PORT = 1235;

    private JTextField portTextField;
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
        portTextField = new JTextField("Port: Not running");
        portTextField.setEditable(false);

        // Layout setup
        JPanel panel = new JPanel();
        panel.add(startButton);
        panel.add(stopButton);
        panel.add(statusLabel);
        panel.add(portTextField);

        add(panel);

        // Add action listeners
        startButton.addActionListener(e -> {
            if (!serverManager.isRunning()) {
                serverManager.startServer(SERVER_PORT);
                statusLabel.setText("Server is running.");
                portTextField.setText("Port: " + SERVER_PORT);
            } else {
                statusLabel.setText("Server is already running.");
            }
        });

        stopButton.addActionListener(e -> {
            if (serverManager.isRunning()) {
                serverManager.stopServer();
                statusLabel.setText("Server is stopped.");
                portTextField.setText("Port: Not running");
            } else {
                statusLabel.setText("Server is not running.");
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        //SwingUtilities.invokeLater(Main::new);
    }
}