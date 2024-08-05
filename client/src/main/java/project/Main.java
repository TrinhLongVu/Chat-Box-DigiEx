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
        StringBuilder content = new StringBuilder();

        try {
            // URL of the LoadBalancer
            URL url = new URL("http://localhost:8080/connect");

            // Open connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            // Close connections
            in.close();
            conn.disconnect();
        } catch (IOException e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error Test: {0}", e.getMessage());
        }

        JFrame mainFrame = new JFrame();
        mainFrame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        new LoginForm(mainFrame, content.toString());
        mainFrame.setVisible(false);
    }
}