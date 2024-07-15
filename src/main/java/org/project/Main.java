package org.project;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;



public class Main {
    public static void main(String[] args) {
        try
        {
            ServerSocket s = new ServerSocket(3001);
            do
            {
                Socket ss = s.accept(); //synchronous
                new ClientThread(ss).start();
            }
            while (true);
        }
        catch(IOException e)
        {
            System.out.println("There're some error");
        }
    }
}