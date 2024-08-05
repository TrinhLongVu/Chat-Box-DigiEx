package org.project;

public class Main {
    public static void main(String[] args) {
        System.err.println(args[0]);
        try {
            new ServerManager().startServer(Integer.parseInt(args[0]));
        } catch (NumberFormatException e) {
            new ServerManager().startServer(1234);
            System.out.println("invalid number");
        }
    }
}