import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginForm extends JDialog {
    private JTextField tfEmail;
    private JButton btnOK;
    private JButton btnCancel;
    private JPanel loginPanel;
    public LoginForm(JFrame parent) {
        super(parent);
        setTitle("Login");
        setContentPane(loginPanel);
        setMinimumSize(new Dimension(450,474));
        setModal(true);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = tfEmail.getText();
                System.out.println("Username: " + username);
            }
        });
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Cancel button clicked");
                dispose();
            }
        });
        setVisible(true);

    }

    public static void main(String[] args) {
        LoginForm loginForm = new LoginForm(null);

    }
}
