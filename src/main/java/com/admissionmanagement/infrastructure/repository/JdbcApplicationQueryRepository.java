package com.admissionmanagement.infrastructure.repository;

import com.admissionmanagement.application.analytics.AnalyticsGrouping;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.infrastructure.config.DatabaseConnectionFactory;
import com.admissionmanagement.infrastructure.exception.DataAccessException;
import com.admissionmanagement.projection.ApplicationDetailsProjection;
import com.admissionmanagement.projection.ApplicationEventProjection;
import com.admissionmanagement.projection.ApplicationSubmissionStatsProjection;
import com.admissionmanagement.projection.ApplicationSummaryProjection;
import com.admissionmanagement.projection.ChannelCommunicationResultStatsProjection;
import com.admissionmanagement.projection.CommunicationResultStatsProjection;
import com.admissionmanagement.projection.FinalResultStatsProjection;
import com.admissionmanagement.repository.ApplicationQueryRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class JdbcApplicationQueryRepository implements ApplicationQueryRepository {
    private final DatabaseConnectionFactory connectionFactory;

    public JdbcApplicationQueryRepository(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public List<ApplicationSummaryProjection> findByCriteria(
            List<ApplicationStatus> statuses,
            ApplicationSearchCriteria criteria
    ) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT a.application_id,
                       concat_ws(' ', a.first_name, a.last_name) AS full_name,
                       p.name AS program_name,
                       s.name AS status_name,
                       lc.result AS last_communication_result,
                       a.datetime
                FROM application a
                JOIN educational_program p ON p.program_id = a.program_id
                JOIN application_status s ON s.status_id = a.status_id
                LEFT JOIN LATERAL (
                    SELECT ac.result
                    FROM application_communication ac
                    WHERE ac.application_id = a.application_id
                    ORDER BY ac.datetime DESC, ac.event_id DESC
                    LIMIT 1
                ) lc ON TRUE
                WHERE s.name IN (
                """);
        sql.append(placeholders(statuses.size())).append(")");

        List<SqlParameter> parameters = new ArrayList<>();
        statuses.forEach(status -> parameters.add(SqlParameter.of(status.name())));
        appendCriteria(sql, parameters, criteria);
        sql.append(" ORDER BY a.datetime DESC, a.application_id DESC");

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ApplicationSummaryProjection> applications = new ArrayList<>();
                while (resultSet.next()) {
                    applications.add(new ApplicationSummaryProjection(
                            resultSet.getInt("application_id"),
                            resultSet.getString("full_name"),
                            resultSet.getString("program_name"),
                            ApplicationStatus.valueOf(resultSet.getString("status_name")),
                            nullableCommunicationResult(resultSet, "last_communication_result"),
                            resultSet.getTimestamp("datetime").toLocalDateTime()
                    ));
                }
                return applications;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to find applications by criteria", exception);
        }
    }

    @Override
    public Optional<ApplicationDetailsProjection> findDetailsById(Integer applicationId) {
        String sql = """
                SELECT a.application_id,
                       concat_ws(' ', a.first_name, a.last_name) AS full_name,
                       p.name AS program_name,
                       a.phone,
                       a.email,
                       a.comment,
                       s.name AS status_name,
                       lc.result AS last_communication_result,
                       a.datetime
                FROM application a
                JOIN educational_program p ON p.program_id = a.program_id
                JOIN application_status s ON s.status_id = a.status_id
                LEFT JOIN LATERAL (
                    SELECT ac.result
                    FROM application_communication ac
                    WHERE ac.application_id = a.application_id
                    ORDER BY ac.datetime DESC, ac.event_id DESC
                    LIMIT 1
                ) lc ON TRUE
                WHERE a.application_id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, applicationId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ApplicationDetailsProjection(
                        resultSet.getInt("application_id"),
                        resultSet.getString("full_name"),
                        resultSet.getString("program_name"),
                        resultSet.getString("phone"),
                        resultSet.getString("email"),
                        resultSet.getString("comment"),
                        ApplicationStatus.valueOf(resultSet.getString("status_name")),
                        nullableCommunicationResult(resultSet, "last_communication_result"),
                        resultSet.getTimestamp("datetime").toLocalDateTime()
                ));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to find application details", exception);
        }
    }

    @Override
    public List<ApplicationEventProjection> findEventsByApplicationId(Integer applicationId) {
        return findEvents("WHERE event_source.application_id = ?", List.of(SqlParameter.of(applicationId)));
    }

    @Override
    public List<ApplicationEventProjection> findAllApplicationEvents() {
        return findEvents("", List.of());
    }

    @Override
    public List<ApplicationSubmissionStatsProjection> countByPeriod(
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            AnalyticsGrouping grouping
    ) {
        String datePart = grouping == AnalyticsGrouping.MONTH ? "month" : "day";
        String sql = """
                SELECT date_trunc('%s', a.datetime)::date AS period_start,
                       count(*) AS applications_count
                FROM application a
                WHERE a.datetime >= ? AND a.datetime <= ?
                GROUP BY period_start
                ORDER BY period_start
                """.formatted(datePart);

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(dateFrom));
            statement.setTimestamp(2, Timestamp.valueOf(dateTo));

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ApplicationSubmissionStatsProjection> stats = new ArrayList<>();
                while (resultSet.next()) {
                    Date periodStart = resultSet.getDate("period_start");
                    stats.add(new ApplicationSubmissionStatsProjection(
                            periodStart.toLocalDate(),
                            resultSet.getLong("applications_count")
                    ));
                }
                return stats;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to count applications by period", exception);
        }
    }

    @Override
    public List<CommunicationResultStatsProjection> countByCommunicationResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        String sql = """
                SELECT ac.result,
                       count(*) AS communications_count
                FROM application_communication ac
                WHERE ac.datetime >= ? AND ac.datetime <= ?
                GROUP BY ac.result
                ORDER BY ac.result
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(dateFrom));
            statement.setTimestamp(2, Timestamp.valueOf(dateTo));

            try (ResultSet resultSet = statement.executeQuery()) {
                List<CommunicationResultStatsProjection> stats = new ArrayList<>();
                while (resultSet.next()) {
                    stats.add(new CommunicationResultStatsProjection(
                            CommunicationResult.valueOf(resultSet.getString("result")),
                            resultSet.getLong("communications_count")
                    ));
                }
                return stats;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to count communication results", exception);
        }
    }

    @Override
    public List<ChannelCommunicationResultStatsProjection> countChannelByCommunicationResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        String sql = """
                SELECT ac.channel,
                       ac.result,
                       count(*) AS communications_count
                FROM application_communication ac
                WHERE ac.datetime >= ? AND ac.datetime <= ?
                GROUP BY ac.channel, ac.result
                ORDER BY ac.channel, ac.result
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(dateFrom));
            statement.setTimestamp(2, Timestamp.valueOf(dateTo));

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ChannelCommunicationResultStatsProjection> stats = new ArrayList<>();
                while (resultSet.next()) {
                    stats.add(new ChannelCommunicationResultStatsProjection(
                            CommunicationChannel.valueOf(resultSet.getString("channel")),
                            CommunicationResult.valueOf(resultSet.getString("result")),
                            resultSet.getLong("communications_count")
                    ));
                }
                return stats;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to count communication results by channel", exception);
        }
    }

    @Override
    public List<FinalResultStatsProjection> countByFinalProcessingResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        String sql = """
                SELECT s.name AS status_name,
                       count(*) AS applications_count
                FROM application a
                JOIN application_status s ON s.status_id = a.status_id
                WHERE a.datetime >= ?
                  AND a.datetime <= ?
                  AND s.name IN ('CONFIRMED', 'REJECTED', 'CANCELLED')
                GROUP BY s.name
                ORDER BY s.name
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(dateFrom));
            statement.setTimestamp(2, Timestamp.valueOf(dateTo));

            try (ResultSet resultSet = statement.executeQuery()) {
                List<FinalResultStatsProjection> stats = new ArrayList<>();
                while (resultSet.next()) {
                    stats.add(new FinalResultStatsProjection(
                            ApplicationStatus.valueOf(resultSet.getString("status_name")),
                            resultSet.getLong("applications_count")
                    ));
                }
                return stats;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to count final processing results", exception);
        }
    }

    private List<ApplicationEventProjection> findEvents(String filterClause, List<SqlParameter> parameters) {
        String sql = """
                SELECT event_source.application_id,
                       event_source.event_time,
                       event_source.event_type,
                       event_source.description
                FROM (
                    SELECT ac.application_id,
                           ac.datetime AS event_time,
                           'COMMUNICATION' AS event_type,
                           'Channel: ' || ac.channel
                               || ', result: ' || ac.result
                               || COALESCE(', comment: ' || ac.comment, '') AS description
                    FROM application_communication ac
                    UNION ALL
                    SELECT sc.application_id,
                           sc.datetime AS event_time,
                           'STATUS_CHANGE' AS event_type,
                           'Status changed from ' || ps.name || ' to ' || ns.name
                               || COALESCE(', reason: ' || sc.reason, '') AS description
                    FROM application_status_change sc
                    JOIN application_status ps ON ps.status_id = sc.previous_status_id
                    JOIN application_status ns ON ns.status_id = sc.new_status_id
                ) event_source
                %s
                ORDER BY event_source.event_time DESC
                """.formatted(filterClause);

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<ApplicationEventProjection> events = new ArrayList<>();
                while (resultSet.next()) {
                    events.add(new ApplicationEventProjection(
                            resultSet.getInt("application_id"),
                            resultSet.getTimestamp("event_time").toLocalDateTime(),
                            resultSet.getString("event_type"),
                            resultSet.getString("description")
                    ));
                }
                return events;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Failed to find application events", exception);
        }
    }

    private void appendCriteria(
            StringBuilder sql,
            List<SqlParameter> parameters,
            ApplicationSearchCriteria criteria
    ) {
        if (criteria == null) {
            return;
        }

        if (criteria.phone() != null && !criteria.phone().isBlank()) {
            sql.append(" AND a.phone ILIKE ?");
            parameters.add(SqlParameter.of("%" + criteria.phone() + "%"));
        }
        if (criteria.email() != null && !criteria.email().isBlank()) {
            sql.append(" AND a.email ILIKE ?");
            parameters.add(SqlParameter.of("%" + criteria.email() + "%"));
        }
        if (criteria.lastCommunicationResult() != null) {
            sql.append(" AND lc.result = ?");
            parameters.add(SqlParameter.of(criteria.lastCommunicationResult().name()));
        }
        if (criteria.dateFrom() != null) {
            sql.append(" AND a.datetime >= ?");
            parameters.add(SqlParameter.of(criteria.dateFrom()));
        }
        if (criteria.dateTo() != null) {
            sql.append(" AND a.datetime <= ?");
            parameters.add(SqlParameter.of(criteria.dateTo()));
        }
    }

    private String placeholders(int count) {
        StringJoiner joiner = new StringJoiner(", ");
        for (int index = 0; index < count; index++) {
            joiner.add("?");
        }
        return joiner.toString();
    }

    private void bindParameters(PreparedStatement statement, List<SqlParameter> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            parameters.get(index).bind(statement, index + 1);
        }
    }

    private CommunicationResult nullableCommunicationResult(ResultSet resultSet, String columnName)
            throws SQLException {
        String value = resultSet.getString(columnName);
        if (value == null) {
            return null;
        }
        return CommunicationResult.valueOf(value);
    }

    private record SqlParameter(Object value) {
        private static SqlParameter of(Object value) {
            return new SqlParameter(value);
        }

        private void bind(PreparedStatement statement, int parameterIndex) throws SQLException {
            if (value instanceof LocalDateTime localDateTime) {
                statement.setTimestamp(parameterIndex, Timestamp.valueOf(localDateTime));
            } else if (value instanceof Integer integer) {
                statement.setInt(parameterIndex, integer);
            } else {
                statement.setString(parameterIndex, (String) value);
            }
        }
    }
}
