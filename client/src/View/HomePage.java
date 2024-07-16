package View;

import data.dataChat;
import socket.Send;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.event.ListSelectionEvent;

public class HomePage extends JDialog {

    private JTextArea userArea;
    private JButton btnSend;
    public static DefaultListModel<String> listModel = new DefaultListModel<>();
    private JPanel homePanel;
    private JTextField tfInput;
    private JList<String> chatList;
    private JLabel userLabel;
    public static JList<String> JlistUsers;
    public static DefaultListModel<String> listModelUsers = new DefaultListModel<>();
    public static String selectedUser = "";
    private Socket _socket = null;
    private String myName;

    public HomePage(JFrame parent, Socket socket, String myName) {
        super(parent);
        _socket = socket;
        myName = myName;
        setTitle("Home Page");
        setMinimumSize(new Dimension(450, 474));
        setModal(true);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Initialize components
        homePanel = new JPanel(new BorderLayout());
        chatList = new JList<>(listModel);
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userLabel = new JLabel("chatting with user: ");

        userArea = new JTextArea();
        userArea.setEditable(false);

        tfInput = new JTextField();
        btnSend = new JButton("Send");

        JPanel onlineUser = new JPanel(new BorderLayout());

        JlistUsers = new JList<>(listModelUsers);
        JScrollPane JScrollPaneUsers = new JScrollPane(JlistUsers);

        JlistUsers.addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                selectedUser = JlistUsers.getSelectedValue();
                userLabel.setText("chatting with user: " + selectedUser);

                LinkedList<String> history = dataChat.contentChat.get(selectedUser);
                if(history == null){
                    history = new LinkedList<>();
                    dataChat.contentChat.put(selectedUser, history);
                }
                listModel.clear();

                for(String content: history) {
                    listModel.addElement(content);
                }
            }
        });

        onlineUser.add(JScrollPaneUsers, BorderLayout.CENTER);
        onlineUser.add(new JLabel("Users Online"), BorderLayout.NORTH);

        // Add components to homePanel
        JScrollPane scrollPane = new JScrollPane(chatList);
        scrollPane.setPreferredSize(new Dimension(300, 400));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(tfInput, BorderLayout.CENTER);
        inputPanel.add(btnSend, BorderLayout.EAST);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(userLabel, BorderLayout.NORTH);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        homePanel.add(chatPanel, BorderLayout.CENTER);
        homePanel.add(inputPanel, BorderLayout.SOUTH);
        homePanel.add(onlineUser, BorderLayout.EAST);

        setContentPane(homePanel);

        handleEvent();
        setVisible(true);
    }

    private void handleEvent() {
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        tfInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String message = tfInput.getText();
        if (!message.trim().isEmpty()) {
            listModel.addElement("You: " + message);
            LinkedList<String> history = dataChat.contentChat.get(selectedUser);
            if(history == null){
                history = new LinkedList<>();
            }
            history.add("You" + ": " + message);
            tfInput.setText("");
            new Send(_socket).sendData("type:chat,send:" + myName + ",recieve:" + selectedUser + ",data:" + message);
        }
    }
}
