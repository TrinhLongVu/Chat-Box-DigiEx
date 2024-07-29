package project;

import project.Chat.Receive;
import project.View.LoginForm;

import java.io.IOException;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Main {
    public static void main(String[] args) {

        // Starting listen to load balancing
        final int LOADBALANCING_PORT = 3005;
        final int RECONNECT_DELAY = 5000;
        // Create the main frame to hold the dialog
        JFrame mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        while (true) {
            try {
                Socket socket = new Socket("localhost", LOADBALANCING_PORT);
                // Start receive message about new server port from LB
                new Receive(socket).start();
    
                new LoginForm(mainFrame, socket);
                mainFrame.setVisible(false);
                break;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(mainFrame, "An error occurred, we are trying to reconnect to the server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.out.println("There're some error");
                try {
                    Thread.sleep(RECONNECT_DELAY); // Wait before retrying
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
}