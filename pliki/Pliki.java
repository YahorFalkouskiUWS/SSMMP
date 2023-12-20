package pliki;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class Pliki implements Runnable {
    private final Socket clientSocket;

    public Pliki(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
    }

    public static boolean isValidBase64(String str) {
        return str.matches("[A-Za-z0-9+/=]+");
    }

    @Override
    public void run() {

        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {
            String request = input.readLine();
            String[] requestFile = request.split(";");

            if (requestFile[0].equals("wgraj_plik")) {
                String destinationnazwaPliku = requestFile[1];
                String encodedFile = requestFile[2];
                if (isValidBase64(encodedFile)) {
                    byte[] fileBytes = Base64.getDecoder().decode(encodedFile);

                    // Отримання шляху до робочого каталогу проекту
                    String projectFolderPath = System.getProperty("user.dir");

                    String filesFolderPath = projectFolderPath + File.separator + "inst";

                    // Створення директорії, якщо її не існує
                    File filesFolder = new File(filesFolderPath);
                    if (!filesFolder.exists()) {
                        filesFolder.mkdirs();
                    }

                    // Запис файлу у визначену папку
                    String destinationPath = filesFolderPath + File.separator + destinationnazwaPliku;
                    Files.write(Paths.get(destinationPath), fileBytes);
                    output.println("200;Plik wgrany.");
                } else {
                    output.println("Ошибка: Строка не является валидным Base64.");
                }
            } else if (requestFile[0].equals("pobierz_plik")) {
                String nazwaPliku = requestFile[2];

                // Отримання шляху до робочого каталогу проекту
                String projectFolderPath = System.getProperty("user.dir");

                // Формування шляху до папки files/Serhii відносно робочого каталогу проекту
                String filesFolderPath = projectFolderPath + File.separator + "inst";

                String sciezkaPliku = filesFolderPath + File.separator + nazwaPliku;

                Path path = Paths.get(sciezkaPliku);
                if (Files.exists(path)) {
                    byte[] fileBytes = Files.readAllBytes(path);
                    String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                    output.println("200;" + encodedFile);
                } else {
                    output.println("404;Pliku nie znaleziono.");
                }
            }
        } catch (IOException e) {
            System.err.println("Błąd przetwarzania żądania.");
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Błąd gniazda.");
                }
            }
        }
    }
}
