package com.admissionmanagement.infrastructure.repository;

import com.admissionmanagement.application.analytics.AnalyticsGrouping;
import com.admissionmanagement.domain.application.ApplicationStatus;
import com.admissionmanagement.domain.application.CommunicationChannel;
import com.admissionmanagement.domain.application.CommunicationResult;
import com.admissionmanagement.dto.ApplicationSearchCriteria;
import com.admissionmanagement.dto.LastCommunicationFilterMode;
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

    static SearchQuerySnapshot buildSearchQueryForTest(
            List<ApplicationStatus> statuses,
            ApplicationSearchCriteria criteria
    ) {
        SqlQuery query = new ApplicationSearchQueryBuilder(statuses, criteria).build();
        return new SearchQuerySnapshot(query.sql(), query.parameters().size());
    }

    @Override
    public List<ApplicationSummaryProjection> findByCriteria(
            List<ApplicationStatus> statuses,
            ApplicationSearchCriteria criteria
    ) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        SqlQuery query = new ApplicationSearchQueryBuilder(statuses, criteria).build();
        return executeListQuery(
                query,
                this::mapApplicationSummary,
                "Failed to find applications by criteria"
        );
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

        return executeOptionalQuery(
                new SqlQuery(sql, List.of(SqlParameter.of(applicationId))),
                this::mapApplicationDetails,
                "Failed to find application details"
        );
    }

    @Override
    public List<ApplicationEventProjection> findEventsByApplicationId(Integer applicationId) {
        return findEventProjections(
                "WHERE event_source.application_id = ?",
                List.of(SqlParameter.of(applicationId))
        );
    }

    @Override
    public List<ApplicationEventProjection> findAllApplicationEvents() {
        return findEventProjections("", List.of());
    }

    @Override
    public List<ApplicationSubmissionStatsProjection> countByPeriod(
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            AnalyticsGrouping grouping
    ) {
        String periodDatePart = grouping == AnalyticsGrouping.MONTH ? "month" : "day";
        String sql = """
                SELECT date_trunc('%s', a.datetime)::date AS period_start,
                       count(*) AS applications_count
                FROM application a
                WHERE a.datetime >= ? AND a.datetime <= ?
                GROUP BY period_start
                ORDER BY period_start
                """.formatted(periodDatePart);

        return executeListQuery(
                new SqlQuery(sql, dateRangeParameters(dateFrom, dateTo)),
                this::mapSubmissionStats,
                "Failed to count applications by period"
        );
    }

    @Override
    public List<CommunicationResultStatsProjection> countByCommunicationResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        String sql = """
                SELECT primary_communication.result,
                       count(*) AS communications_count
                FROM (
                    SELECT DISTINCT ON (ac.application_id)
                           ac.application_id,
                           ac.result,
                           ac.datetime
                    FROM application_communication ac
                    ORDER BY ac.application_id, ac.datetime, ac.event_id
                ) primary_communication
                WHERE primary_communication.datetime >= ?
                  AND primary_communication.datetime <= ?
                GROUP BY primary_communication.result
                ORDER BY primary_communication.result
                """;

        return executeListQuery(
                new SqlQuery(sql, dateRangeParameters(dateFrom, dateTo)),
                this::mapCommunicationResultStats,
                "Failed to count communication results"
        );
    }

    @Override
    public List<ChannelCommunicationResultStatsProjection> countChannelByCommunicationResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        String sql = """
                SELECT primary_communication.channel,
                       primary_communication.result,
                       count(*) AS communications_count
                FROM (
                    SELECT DISTINCT ON (ac.application_id)
                           ac.application_id,
                           ac.channel,
                           ac.result,
                           ac.datetime
                    FROM application_communication ac
                    ORDER BY ac.application_id, ac.datetime, ac.event_id
                ) primary_communication
                WHERE primary_communication.datetime >= ?
                  AND primary_communication.datetime <= ?
                GROUP BY primary_communication.channel, primary_communication.result
                ORDER BY primary_communication.channel, primary_communication.result
                """;

        return executeListQuery(
                new SqlQuery(sql, dateRangeParameters(dateFrom, dateTo)),
                this::mapChannelCommunicationResultStats,
                "Failed to count communication results by channel"
        );
    }

    @Override
    public List<FinalResultStatsProjection> countByFinalProcessingResult(
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        String sql = """
                SELECT ns.name AS status_name,
                       count(*) AS applications_count
                FROM application_status_change sc
                JOIN application_status ns ON ns.status_id = sc.new_status_id
                WHERE sc.datetime >= ?
                  AND sc.datetime <= ?
                  AND ns.name IN ('CONFIRMED', 'REJECTED', 'CANCELLED')
                GROUP BY ns.name
                ORDER BY ns.name
                """;

        return executeListQuery(
                new SqlQuery(sql, dateRangeParameters(dateFrom, dateTo)),
                this::mapFinalResultStats,
                "Failed to count final processing results"
        );
    }

    private List<ApplicationEventProjection> findEventProjections(
            String filterClause,
            List<SqlParameter> parameters
    ) {
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

        return executeListQuery(
                new SqlQuery(sql, parameters),
                this::mapApplicationEvent,
                "Failed to find application events"
        );
    }

    private List<SqlParameter> dateRangeParameters(LocalDateTime dateFrom, LocalDateTime dateTo) {
        return List.of(
                SqlParameter.of(dateFrom),
                SqlParameter.of(dateTo)
        );
    }

    private <T> List<T> executeListQuery(
            SqlQuery query,
            RowMapper<T> rowMapper,
            String failureMessage
    ) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(query.sql())) {
            bindParameters(statement, query.parameters());

            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(rowMapper.map(resultSet));
                }
                return rows;
            }
        } catch (SQLException exception) {
            throw new DataAccessException(failureMessage, exception);
        }
    }

    private <T> Optional<T> executeOptionalQuery(
            SqlQuery query,
            RowMapper<T> rowMapper,
            String failureMessage
    ) {
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(query.sql())) {
            bindParameters(statement, query.parameters());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(rowMapper.map(resultSet));
            }
        } catch (SQLException exception) {
            throw new DataAccessException(failureMessage, exception);
        }
    }

    private ApplicationSummaryProjection mapApplicationSummary(ResultSet resultSet) throws SQLException {
        return new ApplicationSummaryProjection(
                resultSet.getInt("application_id"),
                resultSet.getString("full_name"),
                resultSet.getString("program_name"),
                ApplicationStatus.valueOf(resultSet.getString("status_name")),
                nullableCommunicationResult(resultSet, "last_communication_result"),
                resultSet.getTimestamp("datetime").toLocalDateTime()
        );
    }

    private ApplicationDetailsProjection mapApplicationDetails(ResultSet resultSet) throws SQLException {
        return new ApplicationDetailsProjection(
                resultSet.getInt("application_id"),
                resultSet.getString("full_name"),
                resultSet.getString("program_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getString("comment"),
                ApplicationStatus.valueOf(resultSet.getString("status_name")),
                nullableCommunicationResult(resultSet, "last_communication_result"),
                resultSet.getTimestamp("datetime").toLocalDateTime()
        );
    }

    private ApplicationSubmissionStatsProjection mapSubmissionStats(ResultSet resultSet) throws SQLException {
        return new ApplicationSubmissionStatsProjection(
                resultSet.getDate("period_start").toLocalDate(),
                resultSet.getLong("applications_count")
        );
    }

    private CommunicationResultStatsProjection mapCommunicationResultStats(ResultSet resultSet)
            throws SQLException {
        return new CommunicationResultStatsProjection(
                CommunicationResult.valueOf(resultSet.getString("result")),
                resultSet.getLong("communications_count")
        );
    }

    private ChannelCommunicationResultStatsProjection mapChannelCommunicationResultStats(ResultSet resultSet)
            throws SQLException {
        return new ChannelCommunicationResultStatsProjection(
                CommunicationChannel.valueOf(resultSet.getString("channel")),
                CommunicationResult.valueOf(resultSet.getString("result")),
                resultSet.getLong("communications_count")
        );
    }

    private FinalResultStatsProjection mapFinalResultStats(ResultSet resultSet) throws SQLException {
        return new FinalResultStatsProjection(
                ApplicationStatus.valueOf(resultSet.getString("status_name")),
                resultSet.getLong("applications_count")
        );
    }

    private ApplicationEventProjection mapApplicationEvent(ResultSet resultSet) throws SQLException {
        return new ApplicationEventProjection(
                resultSet.getInt("application_id"),
                resultSet.getTimestamp("event_time").toLocalDateTime(),
                resultSet.getString("event_type"),
                resultSet.getString("description")
        );
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

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    private record SqlQuery(String sql, List<SqlParameter> parameters) {
    }

    static record SearchQuerySnapshot(String sql, int parameterCount) {
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

    private static class ApplicationSearchQueryBuilder {
        private final List<ApplicationStatus> statuses;
        private final ApplicationSearchCriteria criteria;
        private final List<SqlParameter> parameters = new ArrayList<>();

        private ApplicationSearchQueryBuilder(
                List<ApplicationStatus> statuses,
                ApplicationSearchCriteria criteria
        ) {
            this.statuses = statuses;
            this.criteria = criteria;
        }

        private SqlQuery build() {
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
                    """);
            appendCommunicationPresenceJoin(sql);
            sql.append("""
                    WHERE s.name IN (
                    """);
            sql.append(placeholders(statuses.size())).append(")");

            statuses.forEach(status -> parameters.add(SqlParameter.of(status.name())));
            appendCriteria(sql);
            sql.append(" ORDER BY a.datetime DESC, a.application_id DESC");

            return new SqlQuery(sql.toString(), parameters);
        }

        private void appendCriteria(StringBuilder sql) {
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
            if (isByResultMode()) {
                sql.append(" AND lc.result = ?");
                parameters.add(SqlParameter.of(criteria.lastCommunicationResult().name()));
            }
            if (isWithoutCommunicationsMode()) {
                sql.append(" AND comm.has_communication IS NULL");
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

        private void appendCommunicationPresenceJoin(StringBuilder sql) {
            if (!isWithoutCommunicationsMode()) {
                return;
            }

            sql.append("""
                    LEFT JOIN LATERAL (
                        SELECT 1 AS has_communication
                        FROM application_communication ac
                        WHERE ac.application_id = a.application_id
                        LIMIT 1
                    ) comm ON TRUE
                    """);
        }

        private boolean isByResultMode() {
            return filterMode() == LastCommunicationFilterMode.BY_RESULT
                    && criteria.lastCommunicationResult() != null;
        }

        private boolean isWithoutCommunicationsMode() {
            return filterMode() == LastCommunicationFilterMode.WITHOUT_COMMUNICATIONS;
        }

        private LastCommunicationFilterMode filterMode() {
            if (criteria == null || criteria.lastCommunicationFilterMode() == null) {
                return LastCommunicationFilterMode.ANY;
            }
            return criteria.lastCommunicationFilterMode();
        }
    }
}
