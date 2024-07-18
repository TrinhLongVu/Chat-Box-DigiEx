package project.View;

import project.socket.Send;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;

public class LoginForm extends JDialog {
    private JTextField tfEmail;
    private JButton btnOK;
    private JButton btnCancel;
    private JPanel loginPanel;
    private Socưket _socket;

    public LoginForm(JFrame parent, Socket socket) {
        super(parent);
        _socket = socket;
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
                String username = tfEmail.getText();
                new Send(_socket).sendData("type:login&&send:" + username);
                dispose();
                new HomePage(null, _socket, username);
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Cancel button clicked");
                dispose();
                System.exit(0);
            }
        });
        setVisible(true);
    }
}
