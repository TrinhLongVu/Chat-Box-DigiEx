package project.View;

import project.data.dataChat;
import project.socket.Send;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;
import java.util.LinkedList;

public class HomePage extends JFrame {

    private JTextArea userArea;
    private JButton btnSend;
    public static DefaultListModel<String> listModel = new DefaultListModel<>();
    private JPanel homePanel;
    private JTextField tfInput;
    private JList<String> chatList;
    private JLabel userLabel;
    public static JList<String> JlistUsers;
    public static DefaultListModel<String> listModelUsers = new DefaultListModel<>();
    private Socket _socket = null;
    private String _myName = "";
    private JButton btnCreateGroup; // Changed from Button to JButton for consistency

    public HomePage(JFrame parent, Socket socket, String myName) {
        _socket = socket;
        _myName = myName;
        setTitle("Home Page");
        setMinimumSize(new Dimension(450, 474));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Initialize components
        homePanel = new JPanel(new BorderLayout());
        chatList = new JList<>(listModel);
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userLabel = new JLabel("Chatting with user: ");

        userArea = new JTextArea();
        userArea.setEditable(false);

        tfInput = new JTextField();
        btnSend = new JButton("Send");
        btnCreateGroup = new JButton("Group");

        JPanel onlineUser = new JPanel(new BorderLayout());

        JlistUsers = new JList<>(listModelUsers);
        JScrollPane JScrollPaneUsers = new JScrollPane(JlistUsers);

        JlistUsers.addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                dataChat.selectedUser = JlistUsers.getSelectedValue();
                userLabel.setText(_myName + " is chatting with user: " + dataChat.selectedUser);

                LinkedList<String> history = dataChat.contentChat.get(dataChat.selectedUser);
                if(history == null){
                    history = new LinkedList<>();
                    dataChat.contentChat.put(dataChat.selectedUser, history);
                }
                listModel.clear();

                for(String content: history) {
                    listModel.addElement(content);
                }
            }
        });

        onlineUser.add(JScrollPaneUsers, BorderLayout.CENTER);
        onlineUser.add(new JLabel("Users Online"), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(chatList);
        scrollPane.setPreferredSize(new Dimension(300, 400));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(tfInput, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2));

        buttonPanel.add(btnSend);
        buttonPanel.add(btnCreateGroup);

        inputPanel.add(buttonPanel, BorderLayout.EAST);

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

        btnCreateGroup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false); // Hide the HomePage
                Group groupDialog = new Group(HomePage.this, _socket, _myName);  // Pass HomePage instance
                groupDialog.setVisible(true); // Show the GroupDialog
                setVisible(true);
            }
        });
    }

    private void sendMessage() {
        String message = tfInput.getText();
        if (!message.trim().isEmpty()) {
            listModel.addElement("You: " + message);
            LinkedList<String> history = dataChat.contentChat.get(dataChat.selectedUser);
            if(history == null){
                history = new LinkedList<>();
            }
            history.add("You" + ": " + message);
            tfInput.setText("");
            new Send(_socket).sendData("type:chat&&send:" + _myName + "&&receive:" + dataChat.selectedUser + "&&data:" + message);
        }
    }
}
