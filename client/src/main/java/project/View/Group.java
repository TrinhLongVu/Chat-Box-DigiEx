package project.View;

import src.lib.Send;
import src.lib.DataSave;

import javax.swing.*;
import java.awt.*;
import java.net.Socket;

public class Group extends JDialog {

    private Socket socket = null;
    private String myName;
    public Group(JFrame parent, Socket socket, String myName) {
        super(parent, "Create Group", true); // true for modal dialog
        this.socket = socket;
        this.myName = myName;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
                System.out.println( selectedUsers.toString() + this.myName);
                new Send(this.socket).sendData("type:group&&receive:" + selectedUsers.toString() + this.myName + "&&" + "send:" + fieldName.getText());
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
            panel.add(checkBox);
        }

        panel.add(submitButton);

        setContentPane(panel);
    }
}
