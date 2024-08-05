package project.Chat;

import src.lib.TypeReceive;
import project.View.HomePage;
import project.View.LoginForm;

import javax.swing.JOptionPane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import src.lib.Helper;
import src.lib.Send;

public class Receive extends Thread {
    public static boolean isClose = false;
    private String receiveMsg = "";
    private BufferedReader br;
    public static Socket socket;

    public Receive(Socket ss) {
        Receive.socket = ss;
        initializeBufferedReader(ss);
    }

    private void initializeBufferedReader(Socket ss) {
        try {
            InputStream is = ss.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "An error occurred: {0} ", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void run() {
        isClose = false;
        try {
            while (!isClose) {
                this.receiveMsg = this.br.readLine();
                if (receiveMsg != null) {
                    TypeReceive data = Helper.FormatData(receiveMsg);
                    if (data.getType().equals("server")) {
                        handleServer(data.getData());
                        return;
                    }
                    MessageHandlerFactory factory = FactoryClientReceive.getFactory(data.getType());
                    if (factory != null) {
                        factory.handle(data, Receive.socket, receiveMsg);
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE,
                    "An error occurred while receiving message: {0}", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred while receiving message: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            isClose = true;
        } catch (IOException e) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Error closing client socket: {0}",
                    e.getMessage());
        }
    }

    private void handleServer(String data) {
        String[] hostAndPort = data.split("@");
        int port;
        String host = hostAndPort[0];

        try {
            port = Integer.parseInt(hostAndPort[1]);
        } catch (NumberFormatException e) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Invalid port number format: {0}",
                    e.getMessage());
            return;
        }

        try {
            Receive.socket.close();
            Socket s = new Socket(host, port);
            new Send(s).sendData("type:login&&send:" + LoginForm.username);
            new HomePage(null, s, LoginForm.username);
            Receive.socket = s;
            initializeBufferedReader(s); // Reinitialize BufferedReader with new socket
        } catch (IOException e) {
            Logger.getLogger(Receive.class.getName()).log(Level.WARNING, "Unable to connect to server: {0}",
                    e.getMessage());
        }
    }
}
