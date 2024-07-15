package org.project.Chat;

import org.project.format.TypeRecieve;
import org.project.Utils.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class Recieve extends Thread {
    String receiveMsg = "";
    BufferedReader br;
    public Recieve(Socket ss) {
        InputStream is = null;
        try {
            is = ss.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        br = new BufferedReader(new InputStreamReader(is));
    }

    public void run() {
        try {
            do {
                this.receiveMsg = this.br.readLine();
                System.out.println("Received : " + receiveMsg);
                TypeRecieve data = helper.FormatData(receiveMsg);

                System.out.println(data.getType());

            }while (true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

