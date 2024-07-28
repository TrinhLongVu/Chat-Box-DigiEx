package project;

import project.Chat.Database;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerManagerUI extends JFrame {
    private JTable serverTable;
    private ServerTableModel tableModel;
    private JButton shutdownButton;

    public ServerManagerUI() {
        setTitle("Server Manager");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        tableModel = new ServerTableModel();
        serverTable = new JTable(tableModel);

        shutdownButton = new JButton("Shutdown Server");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(serverTable), BorderLayout.CENTER);

        add(panel);
    }

    public void updateServerList(CopyOnWriteArrayList<ServerManagerInfo> serverManagerInfoList) {
        tableModel.setServerList(serverManagerInfoList);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerManagerUI ui = new ServerManagerUI();
            ui.setVisible(true);
            ui.updateServerList(Database.serverManagerInfoList);
        });
    }
}
