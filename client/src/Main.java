import View.LoginForm;
import socket.Recieve;

import java.io.IOException;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        try {
            Socket s = new Socket("localhost", 3005);
            new Recieve(s).start();
            LoginForm loginForm = new LoginForm(null, s);
        } catch (IOException e) {
            System.out.println("There're some error");
        }
    }
}