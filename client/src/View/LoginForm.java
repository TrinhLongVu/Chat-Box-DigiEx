package View;

import socket.Send;

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
    private Socket _socket = null;
    public LoginForm(JFrame parent, Socket socket) {
        super(parent);
        _socket = socket;
        setTitle("Login");
        setContentPane(loginPanel);
        setMinimumSize(new Dimension(450,474));
        setModal(true);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = tfEmail.getText();
                new Send(_socket).sendData("type:login,send:" + username);
                dispose();
                new HomePage(null, _socket, username);
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Cancel button clicked");
                dispose();
            }
        });

        setVisible(true);

    }
}
