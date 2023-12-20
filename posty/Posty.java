package posty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import bd.PolaczenieBaza;

public class Posty implements Runnable {
    private final Socket clientSocket;

    public Posty(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request = input.readLine();
            String[] requestData = request.split(";");
            String requestType = requestData[0];

            if (requestType.equals("post")) {
                handlePostRequest(requestData, output);
            } else if (requestType.equals("czytaj-posts")) {
                handleReadPostsRequest(output);
            }
        } catch (IOException e) {
            System.err.println("Błąd: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Błąd: " + e.getMessage());
            }
        }
    }

    private void handlePostRequest(String[] requestData, PrintWriter output) {
        try (Connection connection = PolaczenieBaza.getConnection()) {
            String username = requestData[1];
            String postData = requestData[2];

            int userId = getUserId(connection, username);
            if (userId == -1) {
                output.println("Użytkownik nie istnieje w DB.");
                output.flush();
                return;
            }

            insertPost(connection, userId, postData);

            output.println("Post Pomyślnie dodany.");
            output.flush();
        } catch (SQLException e) {
            System.err.println("Błąd: " + e.getMessage());
        }
    }

    private void handleReadPostsRequest(PrintWriter output) {
        try (Connection connection = PolaczenieBaza.getConnection()) {
            ResultSet resultSet = returnPostResultSet(connection);

            StringBuilder posts = new StringBuilder();
            while (resultSet.next()) {
                String content = resultSet.getString("content");
                int userId = resultSet.getInt("user");
                String tstamp = resultSet.getString("tstamp");

                String uname = getUsername(connection, userId);
                posts.append("Uzytkownik ").append(uname).append("\s\sNapisał:\t").append(content)
                        .append("\t%\tDodano: ").append(tstamp).append("\t%\t\t%\t");
            }
            output.print("299;" + posts);
        } catch (SQLException e) {
            System.err.println("Błąd: " + e.getMessage());
        }
    }

    private int getUserId(Connection connection, String username) throws SQLException {
        PreparedStatement getUserIdStatement = connection.prepareStatement("SELECT id FROM users WHERE username = ?");
        getUserIdStatement.setString(1, username);
        ResultSet resultSet = getUserIdStatement.executeQuery();

        return resultSet.next() ? resultSet.getInt("id") : -1;
    }

    private void insertPost(Connection connection, int userId, String postData) throws SQLException {
        PreparedStatement insertPostStatement = connection.prepareStatement("INSERT INTO posts (user, content) VALUES (?, ?)");
        insertPostStatement.setInt(1, userId);
        insertPostStatement.setString(2, postData);
        insertPostStatement.executeUpdate();
    }

    private ResultSet returnPostResultSet(Connection connection) throws SQLException {
        PreparedStatement returnPostStatement = connection.prepareStatement("SELECT * FROM posts ORDER BY ID DESC LIMIT 10");
        return returnPostStatement.executeQuery();
    }

    private String getUsername(Connection connection, int userId) throws SQLException {
        PreparedStatement getUsernameStatement = connection.prepareStatement("SELECT username FROM users WHERE id = ?");
        getUsernameStatement.setInt(1, userId);
        ResultSet userResultSet = getUsernameStatement.executeQuery();

        return userResultSet.next() ? userResultSet.getString("username") : "";
    }
}