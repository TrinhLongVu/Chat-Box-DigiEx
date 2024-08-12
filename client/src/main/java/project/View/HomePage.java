package project.View;

import src.lib.DataSave;
import src.lib.LogHandler;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

import project.Chat.MessageManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.net.Socket;
import java.util.LinkedList;


public class HomePage extends JFrame {
    private transient LogHandler logger;
    private JTextArea userArea;
    private JButton btnSend;
    public static DefaultListModel<String> listModel = new DefaultListModel<>();
    public static DefaultListModel<String> listModelUsers = new DefaultListModel<>();
    private JPanel homePanel;
    public static JTextField tfInput;
    private JList<String> chatList;
    public static JLabel userLabel = new JLabel();
    public static JList<String> JlistUsers;
    public static Socket socket = null;
    public static String myName = "";
    private JButton btnCreateGroup;

    public HomePage(JFrame parent, String newName) {
        myName = newName;

        setTitle("Home Page");
        setMinimumSize(new Dimension(450, 474));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

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
                    int index = JlistUsers.getSelectedIndex();
                    DataSave.selectedUser =  DataSave.userOnline.get(index);

                    // Update userLabel on the EDT
                    SwingUtilities.invokeLater(() -> {
                        userLabel.setText(myName + " is chatting with user: " + DataSave.selectedUser.split("\\?")[0]);
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
                MessageManager.sendMessage(tfInput.getText());
            }
        });

        tfInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageManager.sendMessage(tfInput.getText());
            }
        });

        btnCreateGroup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false); // Hide the HomePage
                Group groupDialog = new Group(HomePage.this, myName);  // Pass HomePage instance
                groupDialog.setVisible(true); // Show the GroupDialog
                setVisible(true);
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        if (logger != null) {
            logger.close();
        }
    }
}
