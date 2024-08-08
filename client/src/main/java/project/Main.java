package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import project.View.LoginForm;
import project.utls.LoadBalanceManager;

public class Main {
    public static void main(String[] args) {
        LoadBalanceManager loadBalanceManager = new LoadBalanceManager();
        String response = loadBalanceManager.getConnectResponse();

        JFrame mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        new LoginForm(mainFrame, response);
        mainFrame.setVisible(false);
    }
}