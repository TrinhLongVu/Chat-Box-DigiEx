package project;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import project.View.LoginForm;
public class Main {
    public static void main(String[] args) {
        //******** */
        StringBuilder content = new StringBuilder();

        try {
            // URL of the LoadBalancer
            URL url = new URL("http://localhost:8080");

            // Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            try ( // Get the response from the LoadBalancer
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                // Close connections
            } catch (Exception e) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error: {0}", e.getMessage());
            }
            conn.disconnect();


        } catch (IOException e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error: {0}", e.getMessage());
        }

        //********** */
        // Create the main frame to hold the dialog
        JFrame mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        new LoginForm(mainFrame, content.toString());
        mainFrame.setVisible(false);
        // while (true) {
        //     try {

        //         break;
        //     } catch (IOException e) {
        //         JOptionPane.showMessageDialog(mainFrame,
        //                 "An error occurred, we are trying to reconnect to the server: " + e.getMessage(), "Error",
        //                 JOptionPane.ERROR_MESSAGE);
        //         System.out.println("There're some error");
        //         Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error: {0}",
        //                 e.getMessage());

        //     }
        // }
    }
}