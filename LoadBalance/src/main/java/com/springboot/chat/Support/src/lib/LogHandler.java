package src.lib;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogHandler {
    private static final Logger logger = Logger.getLogger(LogHandler.class.getName());
    private static final String LOG_FILE_PATH = "Logger/application.log";
    private BufferedWriter writer;
    private DateTimeFormatter dateTimeFormatter;

    public LogHandler() throws IOException {
        try {
            Files.createDirectories(Paths.get("Logger"));

            FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);

            writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true));
            dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to set up logger", e);
        }
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
