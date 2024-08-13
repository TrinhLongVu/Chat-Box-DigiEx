package src.lib;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Send {
    BufferedWriter bw;

    public Send(Socket ss) {
        OutputStream os = null;
        try {
            os = ss.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bw = new BufferedWriter(new OutputStreamWriter(os));
    }

    public void sendData(String data) throws IOException {
        bw.write(data);
        bw.newLine();
        bw.flush();
    }
}
