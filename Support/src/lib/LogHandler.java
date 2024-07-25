package src.lib;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogHandler {
    private BufferedWriter writer;
    private DateTimeFormatter dateTimeFormatter;

    public LogHandler(String filePath) throws IOException {
        writer = new BufferedWriter(new FileWriter(filePath, true));
        dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    private void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(dateTimeFormatter);
        String logMessage = String.format("%s [%s] %s", timestamp, level, message);
        try {
            writer.write(logMessage);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write log message: " + e.getMessage());
        }
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void warning(String message) {
        log("WARNING", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void close() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close log writer: " + e.getMessage());
        }
    }
}
