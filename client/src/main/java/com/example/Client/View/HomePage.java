package com.example.Client.view;

import com.example.Client.chat.MessageManager;
import com.example.Support.DataSave;
import com.example.Support.LogHandler;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.LinkedList;

public class HomePage extends JFrame {
    private JTextArea userArea;
    private JButton btnSend;
    private JPanel homePanel;
    private JList<String> chatList;
    private JButton btnCreateGroup;

    public static DefaultListModel<String> listModel = new DefaultListModel<>();
    public static DefaultListModel<String> listModelUsers = new DefaultListModel<>();
    public static JTextField tfInput;
    public static JLabel userLabel = new JLabel();
    public static JList<String> JlistUsers;
    public static String myName = "";

    public HomePage(JFrame parent, String newName) {
        myName = newName;
        initializeComponents(parent);
        ListUsers();
        setContentPane(homePanel);
        handleEvent();
        setVisible(true);
    }
    
    private void initializeComponents(JFrame parent) {
        setTitle("Home Page");
        setMinimumSize(new Dimension(450, 474));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Initialize components
        homePanel = new JPanel(new BorderLayout());
        chatList = new JList<>(listModel);
        userArea = new JTextArea();
        tfInput = new JTextField();
        btnSend = new JButton("Send");
        btnCreateGroup = new JButton("Group");
        JlistUsers = new JList<>(listModelUsers);

        JScrollPane JScrollPaneUsers = new JScrollPane(JlistUsers);
        JScrollPane scrollPane = new JScrollPane(chatList);

        JPanel onlineUser = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        JPanel inputPanel = new JPanel(new BorderLayout());
        JPanel chatPanel = new JPanel(new BorderLayout());
        
        userArea.setEditable(false);
        scrollPane.setPreferredSize(new Dimension(300, 400));
        userLabel.setText(myName);
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        onlineUser.add(JScrollPaneUsers, BorderLayout.CENTER);
        onlineUser.add(new JLabel("Users Online"), BorderLayout.NORTH);

        buttonPanel.setLayout(new GridLayout(1, 2));
        buttonPanel.add(btnSend);
        buttonPanel.add(btnCreateGroup);

        inputPanel.add(buttonPanel, BorderLayout.EAST);
        inputPanel.add(tfInput, BorderLayout.CENTER);

        chatPanel.add(userLabel, BorderLayout.NORTH); // Ensure userLabel is added
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        homePanel.add(chatPanel, BorderLayout.CENTER);
        homePanel.add(inputPanel, BorderLayout.SOUTH);
        homePanel.add(onlineUser, BorderLayout.EAST);
    }
    
    private void ListUsers() {
        JlistUsers.addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                String selectedValue = JlistUsers.getSelectedValue();

                if (selectedValue == null && !DataSave.selectedUser.equals("")) {
                    setUserListSelection();
                } else if (selectedValue != null) {
                    printContentChat();
                }
            }
        });
    }

    private void setUserListSelection() {
        for (int i = 0; i < DataSave.userOnline.size(); i++) {
            if (DataSave.userOnline.get(i).equals(DataSave.selectedUser)) {
                JlistUsers.setSelectedIndex(i);
                break;
            }
        }
    }

    private void printContentChat() {
        int index = JlistUsers.getSelectedIndex();
        DataSave.selectedUser = DataSave.userOnline.get(index);

        SwingUtilities.invokeLater(() -> {
            userLabel.setText(myName + " is chatting with user: " + DataSave.selectedUser.split("\\?")[0]);
            saveHistoryContentChat();
        });
    }

    private void saveHistoryContentChat() {
        LinkedList<String> history = DataSave.contentChat.get(DataSave.selectedUser);
        if (history == null) {
            history = new LinkedList<>();
            DataSave.contentChat.put(DataSave.selectedUser, history);
        }
        listModel.clear();
        for (String content : history) {
            listModel.addElement(content);
        }
    }

    private void handleEvent() {
        eventChatMessage();
        eventEnterChat();
        eventCreateGroup();
    }
    
    private void eventChatMessage() {
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageManager.sendMessage(tfInput.getText());
            }
        });
    }
    
    private void eventEnterChat() {
        tfInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageManager.sendMessage(tfInput.getText());
            }
        });
    }

    private void eventCreateGroup() {
        btnCreateGroup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false); // Hide the HomePage
                Group groupDialog = new Group(HomePage.this, myName); // Pass HomePage instance
                groupDialog.setVisible(true); // Show the GroupDialog
                setVisible(true);
            }
        });
    }
}
