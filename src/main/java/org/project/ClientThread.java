package org.project;

import org.project.Chat.Recieve;
import org.project.Chat.Send;

import java.net.Socket;

public class ClientThread extends Thread {
    private Socket ss;
    ClientThread(Socket a) {
        this.ss = a;
    }
    public void run() {
        System.out.print("Talking to client :::: ");
        System.out.println(ss.getPort());
        do
        {
            new Recieve(ss).start();
            new Send(ss).start();
        }
        while (true);
    }
}