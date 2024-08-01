package project.View;

import src.lib.Send;
import src.lib.DataSave;
import src.lib.LogHandler;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomePage extends JFrame {

    private LogHandler logger;
    private JTextArea userArea;
    private JButton btnSend;
    public static DefaultListModel<String> listModel = new DefaultListModel<>();
    public static DefaultListModel<String> listModelUsers = new DefaultListModel<>();
    private JPanel homePanel;
    private JTextField tfInput;
    private JList<String> chatList;
    public static JLabel userLabel = new JLabel();
    public static JList<String> JlistUsers;
    private Socket socket = null;
    private String myName = "";
    private JButton btnCreateGroup;

    public HomePage(JFrame parent, Socket newSocket, String newName) {
        socket = newSocket;
        myName = newName;
        setTitle("Home Page");
        setMinimumSize(new Dimension(450, 474));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        try {
            logger = new LogHandler();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to initialize logger: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(HomePage.class.getName()).log(Level.SEVERE, "Error: {0}", e.getMessage());
            return;
        }

        // Initialize components
        homePanel = new JPanel(new BorderLayout());
        chatList = new JList<>(listModel);
        chatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userLabel.setText(myName);

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
                String selectedValue = JlistUsers.getSelectedValue();
                if (selectedValue == null) {
                    if (!DataSave.selectedUser.equals("")) {
                        int i = 0;
                        for (String online : DataSave.userOnline) {
                            if (online.equals(DataSave.selectedUser)) {
                                JlistUsers.setSelectedIndex(i);
                            }
                            i++;
                        }
                    }
                } else if (selectedValue != null) {
                    DataSave.selectedUser = selectedValue;

                    // Update userLabel on the EDT
                    SwingUtilities.invokeLater(() -> {
                        userLabel.setText(myName + " is chatting with user: " + DataSave.selectedUser);
                        LinkedList<String> history = DataSave.contentChat.get(DataSave.selectedUser);
                        if (history == null) {
                            history = new LinkedList<>();
                            DataSave.contentChat.put(DataSave.selectedUser, history);
                        }
                        listModel.clear();

                        for (String content : history) {
                            listModel.addElement(content);
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
        chatPanel.add(userLabel, BorderLayout.NORTH); // Ensure userLabel is added
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
                Group groupDialog = new Group(HomePage.this, socket, myName);  // Pass HomePage instance
                groupDialog.setVisible(true); // Show the GroupDialog
                setVisible(true);
            }
        });
    }

    private void sendMessage() {
        String message = tfInput.getText();
        if (!message.trim().isEmpty()) {
            listModel.addElement("You: " + message);
            LinkedList<String> history = DataSave.contentChat.get(DataSave.selectedUser);
            if (history == null) {
                history = new LinkedList<>();
            }
            history.add("You: " + message);
            tfInput.setText("");
            try {
                new Send(socket).sendData("type:chat&&send:" + myName + "&&receive:" + DataSave.selectedUser + "&&data:" + message);
                logger.info("Message sent: " + message);
            } catch (IOException e) {
                logger.error("Failed to send message: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "An error occurred while sending message: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (logger != null) {
            logger.close();
        }
    }
}
