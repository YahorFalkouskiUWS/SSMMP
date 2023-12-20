package rejestracja;

import bd.PolaczenieBaza;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Rejestracja implements Runnable {
    private final Socket clientSocket;

    public Rejestracja(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true);
             Connection connection = PolaczenieBaza.getConnection()) {

            String request = input.readLine();
            String[] userData = parseUserData(request);

            if (userData == null) {
                output.println("406;Zła data rejestracji.");
                return;
            }

            String username = userData[0];
            String password = userData[1];

            if (userExists(username, connection)) {
                output.println("409;Użytkownik istnieje w bazie danych.");
            } else {
                registerUser(username, password, connection);
                output.println("200;Pomyślnie zarejestrowano. Gratuluję.");
            }

        } catch (IOException e) {
            System.err.println("Error in Server Socket: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String[] parseUserData(String requestData) {
        String[] userData = requestData.split(";");
        if (userData.length != 3) {
            return null;
        }
        return new String[]{userData[1], userData[2]};
    }

    private boolean userExists(String username, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void registerUser(String username, String password, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
        }
    }
}
