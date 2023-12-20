package login;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SerwisLogowania {
    public static void main(String[] args) {
        Properties configuration = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream("config.properties")) {
            configuration.load(fileInputStream);
        } catch (IOException e) {
            System.err.println("Error loading configuration file");
            return;
        }

        int loginPort = Integer.parseInt(configuration.getProperty("login.service.port"));

        try {
            ServerSocket serverSocket = new ServerSocket(loginPort);
            ExecutorService threadPool = Executors.newCachedThreadPool();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new Logowanie(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("500;Login Service ERROR. " + e.getMessage());
        }
    }
}
