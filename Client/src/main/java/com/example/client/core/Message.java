package com.example.client.core;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;

@Getter
@Setter
@Component
public class Message {
    private BufferedReader buffer;
}
