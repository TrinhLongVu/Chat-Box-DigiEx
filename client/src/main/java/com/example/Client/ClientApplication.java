package com.example.Client;

import com.example.Client.view.LoginForm;
import com.example.Client.utils.LoadBalanceManager;
import javax.swing.*;

public class ClientApplication {
    public static void main(String[] args) {
        LoadBalanceManager loadBalanceManager = new LoadBalanceManager();
        String response = loadBalanceManager.getConnectResponse();

        JFrame mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        new LoginForm(mainFrame, response);
        mainFrame.setVisible(false);
    }
}