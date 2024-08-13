package com.example.Client.view;

import com.example.Client.chat.SocketManager;
import com.example.Support.lib.DataSave;
import com.example.Support.lib.Send;
import java.io.IOException;
import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;


public class Group extends JDialog {

    private String myName;
    public Group(JFrame parent, String newName) {
        super(parent, "Create Group", true); // true for modal dialog
        myName = newName;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Vertical layout

        JLabel nameLabel = new JLabel("Group Name: ");
        JTextField fieldName = new JTextField(20);

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            StringBuilder selectedUsers = new StringBuilder();
            for (Component comp : panel.getComponents()) {
                if (comp instanceof JCheckBox) {
                    JCheckBox checkBox = (JCheckBox) comp;
                    if (checkBox.isSelected()) {
                        selectedUsers.append(checkBox.getText()).append(", ");
                    }
                }
            }
            if (selectedUsers.length() > 0) {
                try {
                    String uniqueID = UUID.randomUUID().toString();
                    new Send(SocketManager.getSocket()).sendData("type:group&&receive:" + selectedUsers.toString().replace(" ", "") + myName + "&&" + "send:" + fieldName.getText() + "?" + uniqueID);
                } catch (IOException ex) {
                    Logger.getLogger(Group.class.getName()).log(Level.SEVERE, "Error: {0}", ex.getMessage());

                }
            } else {
                JOptionPane.showMessageDialog(this, "You have not selected any users!", "Error", JOptionPane.ERROR_MESSAGE);
            }

            setVisible(false);
        });



        JPanel namePanel = new JPanel();
        namePanel.add(nameLabel);
        namePanel.add(fieldName);
        panel.add(namePanel);
        panel.add(new JLabel("Select users:"));

        for (String online : DataSave.userOnline) {
            JCheckBox checkBox = new JCheckBox(online);
            if(!online.contains("?"))
                panel.add(checkBox);
        }

        panel.add(submitButton);

        setContentPane(panel);
    }
}
