package com.admissionmanagement.infrastructure.config;

import com.admissionmanagement.infrastructure.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public record DatabaseProperties(String url, String username, String password) {
    private static final String PROPERTIES_FILE = "application.properties";

    public static DatabaseProperties load() {
        Properties properties = new Properties();

        try (InputStream inputStream = DatabaseProperties.class
                .getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException(PROPERTIES_FILE + " not found in classpath");
            }
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new DataAccessException("Failed to load database configuration", exception);
        }

        return new DatabaseProperties(
                value("DB_URL", properties.getProperty("db.url")),
                value("DB_USERNAME", properties.getProperty("db.username")),
                value("DB_PASSWORD", properties.getProperty("db.password"))
        );
    }

    private static String value(String environmentName, String propertyValue) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return propertyValue;
    }
}
