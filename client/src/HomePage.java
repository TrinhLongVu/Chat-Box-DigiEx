import javax.swing.*;
import java.awt.*;

public class HomePage extends JDialog {

    private JTextArea areaChat;
    private JTextArea userArea;
    private JButton btnSend;
    private JPanel homePanel;
    private JTextField tfInput;

    public HomePage(JFrame parent) {
        super(parent);
        setTitle("Home Page");
        setContentPane(new JPanel());
        setMinimumSize(new Dimension(450,474));
        setModal(true);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }

    public static void main(String[] args) {
        HomePage homePage = new HomePage(null);

    }
}
