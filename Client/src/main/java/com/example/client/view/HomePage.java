package com.example.client.view;

import com.example.client.chat.MessageManager;
import com.example.support.DataSave;
import com.example.client.core.ClientInfo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;

@Component
@RequiredArgsConstructor
public class HomePage extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(HomePage.class);
    public static JLabel userLabel = new JLabel();
    private final MessageManager messageManager;
    public static JList<String> JListUsers;
    private final ClientInfo clientInfo;
    public static JTextField tfInput;
    private JButton btnCreateGroup;
    private final Group group;
    private JButton btnSend;

    public void init() {
        String userName = clientInfo.getUserName();
        setTitle("Home Page");
        setMinimumSize(new Dimension(450, 474));
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Initialize components
        JPanel homePanel = new JPanel(new BorderLayout());
        JList<String> chatList = new JList<>(clientInfo.getMessageList());
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userLabel.setText(userName);

        JTextArea userArea = new JTextArea();
        userArea.setEditable(false);

        tfInput = new JTextField();
        btnSend = new JButton("Send");
        btnCreateGroup = new JButton("Group");

        JPanel onlineUser = new JPanel(new BorderLayout());

        JListUsers = new JList<>(clientInfo.getClientList());
        JScrollPane JScrollPaneUsers = new JScrollPane(JListUsers);

        JListUsers.addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                String selectedValue = JListUsers.getSelectedValue();

                if (selectedValue == null) {
                    if (!DataSave.selectedUser.isEmpty()) {
                        int i = 0;
                        for (String online : DataSave.userOnline) {
                            if (online.equals(DataSave.selectedUser)) {
                                JListUsers.setSelectedIndex(i);
                            }
                            i++;
                        }
                    }
                } else {
                    int index = JListUsers.getSelectedIndex();
                    DataSave.selectedUser =  DataSave.userOnline.get(index);

                    // Update userLabel on the EDT
                    SwingUtilities.invokeLater(() -> {
                        userLabel.setText(userName + " is chatting with user: " + DataSave.selectedUser.split("\\?")[0]);
                        LinkedList<String> history = DataSave.contentChat.computeIfAbsent(DataSave.selectedUser, k -> new LinkedList<>());
                        clientInfo.getMessageList().clear();

                        for (String content : history) {
                            clientInfo.getMessageList().addElement(content);
                        }
                    });
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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log.info("Shutting down application...");
                System.exit(0);
            }
        });

        handleEvent();
        setVisible(true);
    }

    private void handleEvent() {
        String userName = clientInfo.getUserName();

        btnSend.addActionListener(_ -> messageManager.sendMessage(tfInput.getText()));

        tfInput.addActionListener(_ -> messageManager.sendMessage(tfInput.getText()));

        btnCreateGroup.addActionListener(_ -> {
            setVisible(false);
            group.setUserName(userName);
            group.init();
            setVisible(true);
        });
    }
}