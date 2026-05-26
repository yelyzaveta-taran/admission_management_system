package com.admissionmanagement.infrastructure.repository;

import com.admissionmanagement.domain.application.Application;
import com.admissionmanagement.domain.application.ApplicationEvent;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.domain.application.CommunicationEvent;
import com.admissionmanagement.domain.application.StatusChangeEvent;
import com.admissionmanagement.infrastructure.config.DatabaseConnectionFactory;
import com.admissionmanagement.infrastructure.exception.DataAccessException;
import com.admissionmanagement.repository.ApplicationRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

public class JdbcApplicationRepository implements ApplicationRepository {
    private final DatabaseConnectionFactory connectionFactory;

    public JdbcApplicationRepository(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Optional<Application> findById(Integer applicationId) {
        String sql = """
                SELECT a.application_id,
                       a.program_id,
                       a.first_name,
                       a.last_name,
                       a.phone,
                       a.email,
                       a.comment,
                       s.name AS status_name,
                       a.datetime
                FROM application a
                JOIN application_status s ON s.status_id = a.status_id
                WHERE a.application_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, applicationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapApplication(resultSet));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to find application by id", exception);
        }
    }

    @Override
    public void save(Application application) {
        if (application.getApplicationId() == null) {
            throw new IllegalArgumentException("Application id is required for saving existing aggregate");
        }

        try (Connection connection = connectionFactory.getConnection()) {
            executeInTransaction(connection, () -> {
                updateApplicationStatus(connection, application);
                saveUncommittedDomainEvents(connection, application);
            });
            application.clearNewEvents();
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to save application", exception);
        }
    }

    private void executeInTransaction(Connection connection, TransactionalOperation operation)
            throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            operation.execute();
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private Application mapApplication(ResultSet resultSet) throws SQLException {
        return new Application(
                resultSet.getInt("application_id"),
                resultSet.getInt("program_id"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("comment"),
                ApplicationStatus.valueOf(resultSet.getString("status_name")),
                resultSet.getTimestamp("datetime").toLocalDateTime()
        );
    }

    private void updateApplicationStatus(Connection connection, Application application) throws SQLException {
        String sql = """
                UPDATE application
                SET status_id = (
                    SELECT status_id
                    FROM application_status
                    WHERE name = ?
                )
                WHERE application_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, application.getStatus().name());
            statement.setInt(2, application.getApplicationId());
            statement.executeUpdate();
        }
    }

    private void saveUncommittedDomainEvents(Connection connection, Application application) throws SQLException {
        for (ApplicationEvent event : application.getNewEvents()) {
            saveDomainEvent(connection, application.getApplicationId(), event);
        }
    }

    private void saveDomainEvent(
            Connection connection,
            Integer applicationId,
            ApplicationEvent event
    ) throws SQLException {
        if (event instanceof CommunicationEvent communicationEvent) {
            saveCommunicationEvent(connection, applicationId, communicationEvent);
        } else if (event instanceof StatusChangeEvent statusChangeEvent) {
            saveStatusChangeEvent(connection, applicationId, statusChangeEvent);
        }
    }

    private void saveCommunicationEvent(
            Connection connection,
            Integer applicationId,
            CommunicationEvent event
    ) throws SQLException {
        String sql = """
                INSERT INTO application_communication
                    (application_id, channel, result, comment, datetime)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, applicationId);
            statement.setString(2, event.getChannel().name());
            statement.setString(3, event.getResult().name());
            statement.setString(4, event.getComment());
            statement.setTimestamp(5, Timestamp.valueOf(event.getOccurredAt()));
            statement.executeUpdate();
        }
    }

    private void saveStatusChangeEvent(
            Connection connection,
            Integer applicationId,
            StatusChangeEvent event
    ) throws SQLException {
        String sql = """
                INSERT INTO application_status_change
                    (application_id, previous_status_id, new_status_id, reason, datetime)
                VALUES (
                    ?,
                    (SELECT status_id FROM application_status WHERE name = ?),
                    (SELECT status_id FROM application_status WHERE name = ?),
                    ?,
                    ?
                )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, applicationId);
            statement.setString(2, event.getPreviousStatus().name());
            statement.setString(3, event.getNewStatus().name());
            statement.setString(4, event.getReason());
            statement.setTimestamp(5, Timestamp.valueOf(event.getOccurredAt()));
            statement.executeUpdate();
        }
    }

    @FunctionalInterface
    private interface TransactionalOperation {
        void execute() throws SQLException;
    }
}
