package com.example.Client.View;

import javax.swing.*;

import com.example.Client.chat.MessageManager;
import com.example.Client.chat.SocketManager;
import com.example.Client.utils.LoadBalanceManager;
import src.lib.Helper;
import src.lib.Send;

import java.awt.*;
import java.net.Socket;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginForm extends JDialog {
    private JTextField tfEmail;
    private JButton btnOK;
    private JButton btnCancel;
    private JPanel loginPanel;
    public static String userName = "";
    public LoadBalanceManager loadBalanceManager = new LoadBalanceManager();

    public LoginForm(JFrame parent, String content) {
        super(parent);
        setTitle("Login");

        // Initialize components
        loginPanel = new JPanel(new GridLayout(3, 1));
        tfEmail = new JTextField(20);
        btnOK = new JButton("OK");
        btnCancel = new JButton("Cancel");

        // Layout setup
        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(tfEmail);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(btnOK);
        buttonPanel.add(btnCancel);
        loginPanel.add(buttonPanel);

        setContentPane(loginPanel);
        setMinimumSize(new Dimension(450, 150));
        setModal(true);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        btnOK.addActionListener(e -> {
            if (tfEmail.getText().isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Please enter a username.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String data = Helper.FormatData(content).getData();
            if (data == null) {
                JOptionPane.showMessageDialog(parent, "Don't have any available servers.", "Notification", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else {
                userName = tfEmail.getText();
                handleServer(content);
            }
            dispose();
        });

        btnCancel.addActionListener(e -> {
            dispose();
            System.exit(0);
        });
        setVisible(true);
    }
    
    private void handleServer(String receiveMsg) {
        String data = Helper.FormatData(receiveMsg).getData();

        if (data.equals("null")) {
            JOptionPane.showMessageDialog(null, "Don't have any available servers.", "Notification", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] hostAndPort = data.split("@");
        int port;
        String host = hostAndPort[0];

        try {
            port = Integer.parseInt(hostAndPort[1]);
        } catch (NumberFormatException e) {
            Logger.getLogger(LoginForm.class.getName()).log(Level.SEVERE, "Invalid port number format: {0}",
                    e.getMessage());
            return;
        }

        try {
            Socket s = new Socket(host, port);
            if (s.isConnected()) {
                SocketManager.setSocket(s);
                loadBalanceManager.notifyConnected(host, port, userName);
            }

            new MessageManager(s).start();
            new Send(s).sendData("type:login&&send:" + userName);
            new HomePage(null, userName);
        } catch (IOException e) {
            Logger.getLogger(LoginForm.class.getName()).log(Level.WARNING, "Unable to connect to server: {0}",
                    e.getMessage());
        }
    }
}
