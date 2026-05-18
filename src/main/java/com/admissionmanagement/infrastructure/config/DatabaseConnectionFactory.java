package com.admissionmanagement.infrastructure.config;

import com.admissionmanagement.infrastructure.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionFactory {
    private final DatabaseProperties properties;

    public DatabaseConnectionFactory(DatabaseProperties properties) {
        this.properties = properties;
    }

    public static DatabaseConnectionFactory fromApplicationProperties() {
        return new DatabaseConnectionFactory(DatabaseProperties.load());
    }

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    properties.url(),
                    properties.username(),
                    properties.password()
            );
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to open database connection", exception);
        }
    }
}
