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
import java.util.logging.Logger;
import src.lib.Helper;
import src.lib.Send;

public class Receive extends Thread {
    private String receiveMsg = "";
    private BufferedReader br;
    private Socket socket;

    public Receive(Socket ss) {
        this.socket = ss;
        InputStream is;
        try {
            is = ss.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger(Receive.class.getName()).log(null, "An error occurred: {0} ", e.getMessage());

            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void run() {
        try {
            while (true) {
                this.receiveMsg = this.br.readLine();
                if (receiveMsg != null) {
                    System.out.println("Received: " + receiveMsg);
                    TypeReceive data = Helper.FormatData(receiveMsg);
                    if (data.getType().equals("server")) {
                        handleServer(data.getData());
                        return;
                    }
                    MessageHandlerFactory factory = FactoryClientReceive.getFactory(data.getType());
                    if (factory != null) {
                        factory.handle(data, this.socket, receiveMsg);
                    } else {

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger(Receive.class.getName()).log(null, "An error occurred: {0}", e.getMessage());

            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleServer(String data) {
        String[] hostAndPort = data.split("@");
        System.out.println(hostAndPort[0] + "...." + hostAndPort[1]);
        int port;
        String host = hostAndPort[0];

        try {
            port = Integer.parseInt(hostAndPort[1]);
        } catch (NumberFormatException e) {
            Logger.getLogger(Receive.class.getName()).log(null, "Invalid port number format: {0}", e.getMessage());

            System.out.println("Invalid port number format");
            return;
        }
        Socket s = null;
        try {
            this.socket.close();
            s = new Socket(host, port);
            new Receive(s).start();
            new Send(s).sendData("type:login&&send:" + LoginForm.username);
            new HomePage(null, s, LoginForm.username);
        } catch (IOException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
            Logger.getLogger(Receive.class.getName()).log(null, "Unable to connect to server: {0}" , e.getMessage());
        }
    }
}