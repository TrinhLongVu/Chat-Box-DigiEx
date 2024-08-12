package project;

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