package login;

import bd.PolaczenieBaza;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Logowanie implements Runnable {
    private final Socket klientSocket;

    public Logowanie(Socket klientSocket) {
        this.klientSocket = klientSocket;
    }

    @Override
    public void run() {
        BufferedReader input = null;
        PrintWriter output = null;

        try {
            input = new BufferedReader(new InputStreamReader(klientSocket.getInputStream()));
            output = new PrintWriter(klientSocket.getOutputStream(), true);

            String request = input.readLine();
            String[] userData = request.split(";");
            if (userData.length != 3) {
                output.println("Invalid authentication data.");
                return;
            }

            String username = userData[1];
            String password = userData[2];
            Connection connection = PolaczenieBaza.getConnection();
            if (connection != null) {
                String query = "SELECT * FROM users WHERE username = ?";
                try (PreparedStatement checkUserStatement = connection.prepareStatement(query)) {
                    checkUserStatement.setString(1, username);
                    ResultSet resultSet = checkUserStatement.executeQuery();

                    if (resultSet.next()) {
                        String storedPassword = resultSet.getString("password");
                        if (storedPassword.equals(password)) {
                            output.println("200;Logged in");
                        } else {
                            output.println("Incorrect password.");
                        }
                    } else {
                        output.println("User does not exist in the database.");
                    }
                } catch (SQLException e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        System.err.println("Error closing connection: " + e.getMessage());
                    }
                }
            } else {
                output.println("Error connecting to the database.");
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                klientSocket.close();
            } catch (IOException e) {
                System.err.println("Socket error: " + e.getMessage());
            }
        }
    }
}
