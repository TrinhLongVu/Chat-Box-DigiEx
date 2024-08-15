package com.example.LoadBalance.exception;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandler {
    public static void sendNotFound(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String errorMessage = """
                              HTTP/1.1 404 File Not Found\r
                              Content-Type: text/html\r
                              Content-Length: 23\r
                              \r
                              <h1>404 Not Found</h1>""";

        Logger.getLogger(ExceptionHandler.class.getName()).log(Level.WARNING, "Method not supported: {0}", errorMessage);

        out.println(errorMessage);
        out.flush();
        dataOut.write(errorMessage.getBytes());
        dataOut.flush();
    }

    public static void sendNotImplemented(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        String errorMessage = """
                              HTTP/1.1 501 Not Implemented\r
                              Content-Type: text/html\r
                              Content-Length: 25\r
                              \r
                              <h1>501 Not Implemented</h1>""";

        Logger.getLogger(ExceptionHandler.class.getName()).log(Level.WARNING, "Method not supported: {0}", errorMessage);

        out.println(errorMessage);
        out.flush();
        dataOut.write(errorMessage.getBytes());
        dataOut.flush();
    }


}
