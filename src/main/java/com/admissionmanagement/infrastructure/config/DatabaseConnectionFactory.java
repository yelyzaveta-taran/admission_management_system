package com.admissionmanagement.infrastructure.config;

import com.admissionmanagement.infrastructure.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnectionFactory {
    private static final String APPLICATION_TIME_ZONE = "Europe/Kyiv";

    private final DatabaseProperties properties;

    public DatabaseConnectionFactory(DatabaseProperties properties) {
        this.properties = properties;
    }

    public static DatabaseConnectionFactory fromApplicationProperties() {
        return new DatabaseConnectionFactory(DatabaseProperties.load());
    }

    public Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(
                    properties.url(),
                    properties.username(),
                    properties.password()
            );
            setApplicationTimeZone(connection);
            return connection;
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to open database connection", exception);
        }
    }

    private void setApplicationTimeZone(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET TIME ZONE '" + APPLICATION_TIME_ZONE + "'");
        }
    }
}
