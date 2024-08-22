package com.example.client;

import com.example.client.view.LoginForm;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import javax.swing.*;

@SpringBootApplication
public class ClientApplication {
    public static void main(String[] args) {
        // need head full to run GUI
        System.setProperty("java.awt.headless", "false");

        ApplicationContext context = SpringApplication.run(ClientApplication.class, args);
        SwingUtilities.invokeLater(() -> {
            context.getBean(LoginForm.class).init();
        });
    }
}