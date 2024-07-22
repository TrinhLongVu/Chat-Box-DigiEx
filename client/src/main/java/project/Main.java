package project;
import project.Chat.Receive;
import project.View.LoginForm;

import java.io.IOException;
import java.net.Socket;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        try {
            // Create the main frame to hold the dialog
            JFrame mainFrame = new JFrame();
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            Socket s = new Socket("localhost", 3005);
            new Receive(s).start();

            LoginForm loginForm = new LoginForm(mainFrame, s);
            mainFrame.setVisible(false);

            loginForm.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    System.exit(0); 
                }
            });

        } catch (IOException e) {
            System.out.println("There're some error");
        }
    }
}