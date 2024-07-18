package project.socket;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Send extends Thread {
    BufferedWriter bw;

    public Send(Socket ss) {
        OutputStream os;
        try {
            os = ss.getOutputStream();
            bw = new BufferedWriter(new OutputStreamWriter(os));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendData(String data) {
        try {
            bw.write(data);
            bw.newLine();
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {
            DataInputStream din = new DataInputStream(System.in);
            while (true) {
                String k = din.readLine();
                if (k != null) {
                    bw.write(k);
                    bw.newLine();
                    bw.flush();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
