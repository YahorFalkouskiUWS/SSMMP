import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;

public class Interfejs {
    private static String obecnyUser = "";
    private static String celnazwaPliku = "";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (obecnyUser.equals("")) {
                System.out.println("""
                        \n1) Register
                        2) Login
                        3) Wyjść
                        """
                );
            } else {
                System.out.print("""
                        \n1) Napisz post
                        2) Wypisz 10 ostatnich postów
                        3) Wyślij plik na serwer
                        4) Pobrać plik
                        5) Wyloguj
                        6) Wyjść
                                            
                        Your choice:\s"""
                );
            }

            String choice = scanner.nextLine();

            switch (choice) {
                case "1", "2", "3", "4", "5", "6":
                    // Handle other choices
                    break;
                default:
                    System.out.println("Wybierz prawidłową opcję");
                    continue;
            }

            String typ;
            String dane;
            String login = "";
            String haslo;

            switch (choice) {
                case "1":
                    if (obecnyUser.equals("")) {
                        typ = "rejestracja";
                        System.out.print("Login: ");
                        login = scanner.nextLine();
                        System.out.print("Hasło: ");
                        haslo = scanner.nextLine();
                        dane = login + ";" + haslo;
                    } else {
                        typ = "post";
                        System.out.print("\nTreść postu:");
                        dane = obecnyUser + ";" + scanner.nextLine();
                    }
                    break;
                case "2":
                    if (obecnyUser.equals("")) {
                        typ = "logowanie";
                        System.out.print("Login: ");
                        login = scanner.nextLine();
                        System.out.print("Hasło: ");
                        haslo = scanner.nextLine();
                        dane = login + ";" + haslo;
                    } else {
                        typ = "czytaj-posts";
                        System.out.println("\nPosty:");
                        dane = "";
                    }
                    break;
                case "3":
                    if (obecnyUser.equals("")) {
                        System.out.println("The End");
                        return;
                    }
                    typ = "wgraj_plik";
                    System.out.print("File path: ");
                    String sciezkaPliku = scanner.nextLine();
                    if (sciezkaPliku.equals("")) {
                        System.out.println("Podaj ścieżkę");
                        continue;
                    }
                    System.out.print("File name: ");
                    celnazwaPliku = scanner.nextLine();
                    dane = obecnyUser + ";" + sciezkaPliku + ";" + celnazwaPliku; // Додайте celnazwaPliku у рядок запиту
                    break;
                case "4":
                    if (obecnyUser.equals("")) {
                        System.out.println("Before doing this, please log in!");
                        continue;
                    }
                    typ = "pobierz_plik";
                    System.out.print("File name you want to download: ");
                    String nazwaPliku = scanner.nextLine();
                    dane = obecnyUser + ";" + nazwaPliku;
                    break;
                case "5":
                    if (obecnyUser.equals("")) {
                        System.out.println("You are not logged in!");
                        continue;
                    }
                    obecnyUser = "";
                    System.out.println("You have been logged out");
                    continue;
                case "6":
                    System.out.println("The End");
                    return;
                default:
                    System.out.println("Please choose a valid option");
                    continue;
            }

            String request = typ + ";" + dane;

            String response = sendRequestToApiGateway(request);

            String[] responseParts = response.split(";", 2);
            String responseType = responseParts[0];
            String responseData = responseParts.length > 1 ? responseParts[1] : "";

            if (responseType.equals("200")) {
                if (typ.equals("logowanie") || typ.equals("rejestracja")) {
                    obecnyUser = login;
                } else if (typ.equals("wgraj_plik") || typ.equals("pobierz_plik")) {
                    System.out.println("File transfer successful.");
                }
            }
            if (responseType.equals("299")) {
                String[] posts = responseData.split("\t%\t");
                for (String post : posts) {
                    System.out.println(post);
                }
                continue;
            }
            System.out.println(responseData);
        }
    }

    private static String sendRequestToApiGateway(String request) {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Plik konfiguracyjny nie załadował się.");
            return "ERROR";
        }
        int apiGatewayPort = Integer.parseInt(properties.getProperty("api.gateway.port"));
        String apiGatewayIP = properties.getProperty("api.gateway.ip");

        String[] requestPart = request.split(";");
        if (requestPart[0].equals("wgraj_plik")) {
            String sciezkaPliku = requestPart[2];
            String[] fileData = sciezkaPliku.split("\\\\"); // Розділення шляху файлу на компоненти
            String fileName = fileData[fileData.length - 1]; // Останній компонент є ім'ям файлу

            try {
                byte[] fileBytes = Files.readAllBytes(Paths.get(sciezkaPliku));
                String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                String savePath = "files" + File.separator + fileName; // Шлях для збереження файлу в папці files
                Files.write(Paths.get(savePath), fileBytes);

                // Оновлений запит, що містить ім'я файлу для сервера
                request = requestPart[0] + ";" + requestPart[1] + ";" + fileName + ";" + encodedFile;
            } catch (IOException e) {
                System.err.println("Błąd odczytu pliku." + e.getMessage());
                return "Błąd odczytu pliku";
            }
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                output.println(request);
                return input.readLine();
            } catch (IOException e) {
                System.err.println("Błąd połączenia z ApiGateway." + e.getMessage());
                return "Błąd połączenia z ApiGateway.";
            }
        } else if (requestPart[0].equals("pobierz_plik")) {
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                output.println(request);
                String response = input.readLine();
                String[] responseParts = response.split(";", 2);

                if ("200".equals(responseParts[0])) {
                    String encodedFile = responseParts[1];
                    byte[] fileBytes = Base64.getDecoder().decode(encodedFile);
                    String nazwaPliku = requestPart[2];

                    // Динамічне розташування файлу залежно від ім'я користувача
                    String userFolderPath = "files" + File.separator + obecnyUser;
                    File userFolder = new File(userFolderPath);
                    if (!userFolder.exists()) {
                        userFolder.mkdirs();
                    }
                    String celPath = userFolderPath + File.separator + "DOWNLOADED_" + nazwaPliku;
                    try {
                        Files.write(Paths.get(celPath), fileBytes);
                        return "200;Pobranie pliku zakończone pomyślnie: " + celPath;
                    } catch (IOException e) {
                        System.err.println("Błąd zapisu pliku" + e.getMessage());
                        return "Błąd zapisu pliku";
                    }
                } else {
                    return response;
                }
            } catch (IOException e) {
                System.err.println("Błąd połączenia z ApiGateway." + e.getMessage());
                return "Błąd połączenia z ApiGateway.";

            }
        } else {
            try (Socket socket = new Socket(apiGatewayIP, apiGatewayPort);
                 PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                output.println(request);
                return input.readLine();
            } catch (IOException e) {
                System.err.println("Błąd połączenia z ApiGateway." + e.getMessage());
                return "503;Błąd połączenia z ApiGateway.";

            }
        }
    }
}