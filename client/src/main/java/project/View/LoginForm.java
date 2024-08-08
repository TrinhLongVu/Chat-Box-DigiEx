package project.View;

import javax.swing.*;

import src.lib.Helper;
import src.lib.Send;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import project.Chat.Receive;

public class LoginForm extends JDialog {
    private JTextField tfEmail;
    private JButton btnOK;
    private JButton btnCancel;
    private JPanel loginPanel;

    static public String username = "";

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

        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (tfEmail.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(parent, "Please enter a username.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String data = Helper.FormatData(content).getData();
                if (data == null) {
                    JOptionPane.showMessageDialog(parent, "Don't have any available servers.", "Notification", JOptionPane.INFORMATION_MESSAGE);
                    return;
                } else {
                    username = tfEmail.getText();
                    handleServer(content);
                }

                dispose();
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                System.exit(0);
            }
        });
        setVisible(true);
    }
    
    private void handleServer(String receiveMsg) {
        String data = Helper.FormatData(receiveMsg).getData();

        if (data.equals("null")) {
            JOptionPane.showMessageDialog(null, "All servers are full right now.", "Notification", JOptionPane.INFORMATION_MESSAGE);
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
                notifyConnected(host, port, LoginForm.username);
            }
            new Receive(s).start();
            new Send(s).sendData("type:login&&send:" + LoginForm.username);
            new HomePage(null, s, LoginForm.username);
        } catch (IOException e) {
            Logger.getLogger(LoginForm.class.getName()).log(Level.WARNING, "Unable to connect to server: {0}",
                    e.getMessage());
        }
    }

    private void notifyConnected(String host, int port, String name) {
        try {
            URL loadBalancerUrl = new URL("http://localhost:8080/login");

            HttpURLConnection loadBalancerConn = (HttpURLConnection) loadBalancerUrl.openConnection();
            loadBalancerConn.setRequestMethod("POST");
            loadBalancerConn.setDoOutput(true);

            // Message indicating successful connection to the server
            String confirmationMessage = name + "&&" + host + "@"+ port;
            try (OutputStream os = loadBalancerConn.getOutputStream()) {
                os.write(confirmationMessage.getBytes());
                os.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(loadBalancerConn.getInputStream()));
            StringBuilder newContent = new StringBuilder();

            String inputLine;
            
            while ((inputLine = in.readLine()) != null) {
                newContent.append(inputLine);
            }

            // Close connections
            in.close();
            loadBalancerConn.disconnect();
        } catch (IOException e) {
            Logger.getLogger(LoginForm.class.getName()).log(Level.SEVERE, "Error sending confirmation to LoadBalancer: {0}",
                    e.getMessage());
        }
    }
}
