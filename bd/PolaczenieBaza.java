package bd;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class PolaczenieBaza {
    private static final String CONFIG_FILE = "config.properties";

    public static Connection getConnection() throws SQLException {
        Properties properties = loadProperties();
        String url = Objects.requireNonNull(properties.getProperty("database.url"));
        String username = Objects.requireNonNull(properties.getProperty("database.username"));
        String password = Objects.requireNonNull(properties.getProperty("database.password"));
        return DriverManager.getConnection(url, username, password);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(CONFIG_FILE)) {
            properties.load(fileInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration file: " + e.getMessage(), e);
        }
        return properties;
    }
}
