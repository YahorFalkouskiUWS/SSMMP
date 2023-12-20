package apigateway;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import managment.Agent;
import managment.Manager;
import managment.ServiceInfo;

public class ApiGateway {

    private static Agent agent;

    public static void main(String[] args) {
        Properties properties = loadProperties("config.properties");
        int gatewayPort = Integer.parseInt(properties.getProperty("api.gateway.port"));
        int registrationPort = Integer.parseInt(properties.getProperty("registration.service.port"));
        int loginPort = Integer.parseInt(properties.getProperty("login.service.port"));
        int filePort = Integer.parseInt(properties.getProperty("file.service.port"));
        int postPort = Integer.parseInt(properties.getProperty("post.service.port"));


        Manager manager = new Manager();
        agent = new Agent(manager);

        manager.startService("rejestracja", "localhost", registrationPort);
        manager.startService("logowanie", "localhost", loginPort);
        manager.startService("wgraj_plik", "localhost", filePort);
        manager.startService("pobierz_plik", "localhost", filePort);
        manager.startService("post", "localhost", postPort);
        manager.startService("czytaj-posts", "localhost", postPort);


        ExecutorService executorService = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(gatewayPort)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> processRequest(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Błąd połączenia z ApiGateway.");
        } finally {
            executorService.shutdown();
        }
    }

    private static Properties loadProperties(String fileName) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(fileName)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Plik konfiguracyjny nie załadował się.");
        }
        return properties;
    }

    private static void processRequest(Socket clientSocket) {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = input.readLine();
            String[] requestParts = request.split(";", 2);
            String requestType = requestParts[0];

            ServiceInfo serviceInfo = agent.getServiceInfo(requestType);
            if (serviceInfo == null) {
                System.err.println("Service not found: " + requestType);
                return;
            }

            String targetServiceIP = serviceInfo.getIpAddress();
            int targetPort = serviceInfo.getPort();

            if ("wgraj_plik".equals(requestType) || "pobierz_plik".equals(requestType)) {
                handleFileRequest(request, targetServiceIP, targetPort, output);
                return;
            }

            try (Socket targetSocket = new Socket(targetServiceIP, targetPort)) {
                PrintWriter targetOutput = new PrintWriter(targetSocket.getOutputStream(), true);
                BufferedReader targetInput = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()));

                targetOutput.println(request);
                System.out.println("Otrzymano połączenie");
                String response = targetInput.readLine();
                output.print(response);
                output.flush();
            } catch (IOException e) {
                System.err.println("Błędne żądanie przekierowania.");
            }
        } catch (IOException e) {
            System.err.println("Błąd przetwarzania żądania");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Wewnętrzny błąd serwera: " + e.getMessage());
            }
        }
    }

    private static void handleFileRequest(String request, String targetServiceIP, int targetPort, PrintWriter output) {
        try (Socket targetSocket = new Socket(targetServiceIP, targetPort);
             PrintWriter targetOutput = new PrintWriter(targetSocket.getOutputStream(), true);
             BufferedReader targetInput = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))) {

            String[] parts = request.split(";");
            String destinationFileName = parts[parts.length - 2];
            String encodedFile = parts[parts.length - 1];

            targetOutput.println("wgraj_plik;" + destinationFileName + ";" + encodedFile);

            String response = targetInput.readLine();
            output.print(response);
            output.flush();
        } catch (IOException e) {
            System.err.println("Błędne przekazanie plików: " + e.getMessage());
        }
    }
}
