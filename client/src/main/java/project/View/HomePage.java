package project.View;

import src.lib.Send;
import src.lib.DataSave;
import src.lib.LogHandler;
import src.lib.Helper;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;

import project.Main;
import project.Chat.Receive;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.Socket;
import java.net.URL;
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
    public static Socket socket = null;
    private String myName = "";
    private JButton btnCreateGroup;
    private int PORT = 0;

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
                if (socket != null && !socket.isClosed()) {
                    new Send(socket).sendData("type:chat&&send:" + myName + "&&receive:" + DataSave.selectedUser + "&&data:" + message);
                    logger.info("Message sent: " + message);
                }
            } catch (SocketException se) {
                // Clean up old Receive
                Receive.cleanup();
                Receive.isClose = true;

                // Reconnect to new server
                Socket newSocket = reconnectServer();
                new Receive(newSocket).start();
            } catch (IOException e) {
                logger.error("Failed to send message: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "An error occurred while sending message, trying to reconnect to new server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }




    public Socket reconnectServer() {
        Socket newSocket = Receive.socket;
        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL("http://localhost:8080/connect");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
            }

            String data = Helper.FormatData(content.toString()).getData();
            
            if (data.equals("null")) {
                Logger.getLogger(Receive.class.getName()).log(Level.INFO, "No server available");
                JOptionPane.showMessageDialog(null, "No server available", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return null;
            }

            String[] hostAndPort = data.toString().split("@");
            String host = "localhost";
            int port = Integer.parseInt(hostAndPort[1]);

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            newSocket = new Socket(host, port);
            new Send(newSocket).sendData("type:login&&send:" + myName);
            HomePage.socket = newSocket;

            Logger.getLogger(Receive.class.getName()).log(Level.INFO, "Reconnected to new server: " + host + ":" + port);
            JOptionPane.showMessageDialog(null, "Reconnected to new server: " + host + ":" + port, "Reconnected", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            Logger.getLogger(Receive.class.getName()).log(Level.SEVERE, "Failed to reconnect to server: {0}", e.getMessage());
            JOptionPane.showMessageDialog(null, "An error occurred while reconnecting to the server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        return newSocket;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (logger != null) {
            logger.close();
        }
    }
}
