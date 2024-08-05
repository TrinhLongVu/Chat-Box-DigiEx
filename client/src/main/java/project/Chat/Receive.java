package project.Chat;

import src.lib.TypeReceive;
import project.View.HomePage;
import project.View.LoginForm;

import javax.swing.JOptionPane;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import src.lib.Helper;
import src.lib.Send;

public class Receive extends Thread {
    private String receiveMsg = "";
    private BufferedReader br;
    private Socket socket;

    public Receive(Socket ss) {
        this.socket = ss;
        initializeBufferedReader(ss);
    }

    private void initializeBufferedReader(Socket ss) {
        try {
            InputStream is = ss.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
        } catch (IOException e) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "An error occurred: {0} ", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("---------Socket: " + socket.getPort());
                this.receiveMsg = this.br.readLine();
                if (receiveMsg != null) {
                    System.out.println("Receive of client: " + receiveMsg);
                    TypeReceive data = Helper.FormatData(receiveMsg);
                    if (data.getType().equals("server")) {
                        handleServer(data.getData());
                    } else {
                        System.out.println("New upcoming message: " + data.getMessage() + " Port: " + data.getPort());
                        MessageHandlerFactory factory = FactoryClientReceive.getFactory(data.getType());
                        if (factory != null) {
                            factory.handle(data, this.socket, receiveMsg);
                        }
                    }
                }
            } catch (IOException e) {
                Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "An error occurred while receiving message: {0}", e.getMessage());
                JOptionPane.showMessageDialog(null, "An error occurred while receiving message: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                reconnectServer();
            }
        }
    }

    public Socket reconnectServer() {
        Socket newSocket = this.socket;
        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL("http://localhost:8080/connect");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            }

            String data = Helper.FormatData(content.toString()).getData();
            String[] hostAndPort = data.toString().split("@");
            String host = "localhost";
            int port = Integer.parseInt(hostAndPort[1]);

            System.out.print("New Host: " + host);
            System.out.println("...New Port: " + port);

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            newSocket = new Socket(host, port);
            System.out.println("New Socket is created: " + newSocket);
            Logger.getLogger(Receive.class.getName()).log(Level.INFO, "Reconnected to new server: " + host + ":" + port);

            // Reinitialize the Receive thread with the new socket
            this.socket = newSocket;
            initializeBufferedReader(newSocket);

            JOptionPane.showMessageDialog(null, "Reconnected to new server: " + host + ":" + port, "Reconnected", JOptionPane.INFORMATION_MESSAGE);
 
        } catch (IOException e) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Failed to reconnect to server: {0}", e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred while reconnecting to the server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return newSocket;
    }

    private void handleServer(String data) {
        String[] hostAndPort = data.split("@");
        System.out.println("connect host: " + hostAndPort[0] + "@" + hostAndPort[1]);
        int port;
        String host = hostAndPort[0];

        try {
            port = Integer.parseInt(hostAndPort[1]);
        } catch (NumberFormatException e) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Invalid port number format: {0}", e.getMessage());
            System.out.println("Invalid port number format");
            return;
        }

        try {
            this.socket.close();
            Socket s = new Socket(host, port);
            new Send(s).sendData("type:login&&send:" + LoginForm.username);
            new HomePage(null, s, LoginForm.username);
            this.socket = s;
            initializeBufferedReader(s); // Reinitialize BufferedReader with new socket
        } catch (IOException e) {
            System.out.println("Unable to connect to server: " + e.getMessage());
            Logger.getLogger(Receive.class.getName()).log(Level.WARNING, "Unable to connect to server: {0}" , e.getMessage());
        }
    }
}
