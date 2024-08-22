package com.example.client.view;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import com.example.client.chat.SocketManager;
import com.example.support.DataSave;
import com.example.support.Send;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import javax.swing.*;
import java.util.UUID;

@Setter
@Component
@RequiredArgsConstructor
public class Group extends JDialog {
    private static final Logger log = LogManager.getLogger(Group.class);
    private final SocketManager socketManager;
    private String userName;

    public void init() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(400, 300);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Vertical layout

        JLabel nameLabel = new JLabel("Group Name: ");
        JTextField fieldName = new JTextField(20);

        JButton submitButton = getjButton(panel, fieldName);


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

        this.setVisible(true);
    }

    private JButton getjButton(JPanel panel, JTextField fieldName) {
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(e -> {
            StringBuilder selectedUsers = new StringBuilder();
            for (java.awt.Component comp : panel.getComponents()) {
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
                    new Send(socketManager.getSocket()).sendData("type:group&&receive:" + selectedUsers.toString().replace(" ", "") + userName + "&&" + "send:" + fieldName.getText() + "?" + uniqueID);
                } catch (IOException ex) {
                    log.error("Error: {}", ex.getMessage());

                }
            } else {
                JOptionPane.showMessageDialog(this, "You have not selected any users!", "Error", JOptionPane.ERROR_MESSAGE);
            }

            setVisible(false);
        });
        return submitButton;
    }
}
