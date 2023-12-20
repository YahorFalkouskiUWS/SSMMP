package rejestracja;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerwisRejestracji {
    public static void main(String[] args) {
        Properties configuration = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream("config.properties")) {
            configuration.load(fileInputStream);
        } catch (IOException e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
            return;
        }

        int registrationPort = Integer.parseInt(configuration.getProperty("registration.service.port"));

        try {
            ServerSocket serverSocket = new ServerSocket(registrationPort);
            ExecutorService threadPool = Executors.newCachedThreadPool(); // Efficient handling of short-lived tasks

            System.out.println("Serwis Rejestracji running on port " + registrationPort);
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accepts a connection from a client
                threadPool.execute(new Rejestracja(clientSocket)); // Executes the Rejestracja task in the thread pool
            }
        } catch (IOException e) {
            System.err.println("Registration Service ERROR: " + e.getMessage());
        }
    }
}
