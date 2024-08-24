package com.example.loadbalance.database;

import java.util.ArrayList;
import java.util.List;

import com.example.loadbalance.payloads.ClientInfo;
import com.example.loadbalance.payloads.ServerInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class Database {
    private List<ClientInfo> clients ;
    private List<ServerInfo> serverList;
    public Database(){
        this.clients = new ArrayList<>();
        this.serverList = new ArrayList<>();
    }
}
