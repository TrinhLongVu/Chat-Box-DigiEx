package project.View;

import javax.swing.*;

import src.lib.Send;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;

import java.io.IOException;
import java.util.logging.Logger;

public class LoginForm extends JDialog {
    private JTextField tfEmail;
    private JButton btnOK;
    private JButton btnCancel;
    private JPanel loginPanel;
    private Socket socket;

    static public String username = "";

    public LoginForm(JFrame parent, Socket newSocket) {
        super(parent);
        this.socket = newSocket;
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
                username = tfEmail.getText();
                try {
                    new Send(socket).sendData("type:login-load&&send:" + username);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Logger.getLogger(LoginForm.class.getName()).log(java.util.logging.Level.SEVERE,
                            "Error: {0}" , ex.getMessage());

                }
                dispose();
                // new HomePage(null, socket, username);
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
}
