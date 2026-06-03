package com.counterstrike.app.repository;

import com.counterstrike.app.db.Database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class CounterStrikeRepository implements AppRepository {
    private final Database database;

    public CounterStrikeRepository(Database database) {
        this.database = database;
    }

    public TableData findTopPlayers(String search) throws SQLException {
        String sql = """
                SELECT p.PersonID,
                       p.Nickname,
                       p.FullName,
                       pl.Rating,
                       pl.ADR,
                       pl.DPR
                FROM People p
                JOIN Player pl ON p.PersonID = pl.PersonID
                WHERE LOWER(p.FullName) LIKE ?
                   OR LOWER(NVL(p.Nickname, '')) LIKE ?
                   OR TO_CHAR(pl.Rating) LIKE ?
                   OR TO_CHAR(pl.ADR) LIKE ?
                   OR TO_CHAR(pl.DPR) LIKE ?
                ORDER BY pl.Rating DESC NULLS LAST, p.FullName
                """;
        String textLike = like(search);
        String numberLike = numberLike(search);
        return queryTable(
                new String[]{"PlayerID", "Nickname", "Name", "Rating", "ADR", "DPR"},
                sql,
                statement -> {
                    statement.setString(1, textLike);
                    statement.setString(2, textLike);
                    statement.setString(3, numberLike);
                    statement.setString(4, numberLike);
                    statement.setString(5, numberLike);
                }
        );
    }

    public TableData findTrendingTeams(String search) throws SQLException {
        String sql = """
                SELECT t.TeamName,
                       t.Region,
                       NVL(SUM(CASE
                           WHEN m.MatchDate >= ADD_MONTHS(TRUNC(SYSDATE), -3) THEN 1
                           ELSE 0
                       END), 0) AS RecentMatches,
                       NVL(SUM(CASE
                           WHEN m.WinnerTeam = t.TeamName THEN 1
                           ELSE 0
                       END), 0) AS Wins
                FROM Team t
                LEFT JOIN MatchTable m ON m.TeamA = t.TeamName OR m.TeamB = t.TeamName
                WHERE LOWER(t.TeamName) LIKE ?
                   OR LOWER(t.Region) LIKE ?
                GROUP BY t.TeamName, t.Region
                ORDER BY Wins DESC, RecentMatches DESC, t.TeamName
                """;
        String value = like(search);
        return queryTable(
                new String[]{"Team Name", "Region", "Recent Matches", "Wins"},
                sql,
                statement -> {
                    statement.setString(1, value);
                    statement.setString(2, value);
                }
        );
    }

    public TableData findTeamMatchHistory(String search) throws SQLException {
        String sql = """
                SELECT team_a.TeamName || ' vs ' || team_b.TeamName AS MatchName,
                       NVL(mt.MatchResultTeamA, '-') || '-' || NVL(mt.MatchResultTeamB, '-') AS Score,
                       tr.TournamentName,
                       mt.MatchDate,
                       mt.WinnerTeam,
                       mt.Stage
                FROM MatchTable mt
                JOIN Team team_a ON team_a.TeamName = mt.TeamA
                JOIN Team team_b ON team_b.TeamName = mt.TeamB
                JOIN Tournament tr ON tr.TournamentID = mt.TournamentID
                WHERE LOWER(team_a.TeamName) LIKE ?
                   OR LOWER(team_b.TeamName) LIKE ?
                   OR LOWER(tr.TournamentName) LIKE ?
                   OR LOWER(NVL(mt.WinnerTeam, '')) LIKE ?
                   OR LOWER(NVL(mt.Stage, '')) LIKE ?
                ORDER BY mt.MatchDate DESC NULLS LAST, mt.MatchID DESC
                """;
        String value = like(search);
        return queryTable(
                new String[]{"Match", "Score", "Tournament", "Date", "Winner", "Stage"},
                sql,
                statement -> {
                    for (int index = 1; index <= 5; index++) {
                        statement.setString(index, value);
                    }
                }
        );
    }

    public TableData findTournamentMatchSummary(String search) throws SQLException {
        String sql = """
                SELECT tr.TournamentName,
                       tr.TournyStartDate,
                       tr.TournyEndDate,
                       COUNT(mt.MatchID) AS MatchCount
                FROM Tournament tr
                LEFT JOIN MatchTable mt ON mt.TournamentID = tr.TournamentID
                WHERE LOWER(tr.TournamentName) LIKE ?
                GROUP BY tr.TournamentID, tr.TournamentName, tr.TournyStartDate, tr.TournyEndDate
                ORDER BY tr.TournyStartDate DESC NULLS LAST, tr.TournamentID DESC
                """;
        return queryTable(
                new String[]{"Tournament Name", "Start Date", "End Date", "Match Count"},
                sql,
                statement -> statement.setString(1, like(search))
        );
    }

    public TableData findPeople(String search) throws SQLException {
        String sql = """
                SELECT PersonID, Nickname, FullName, Birthday, Nationality
                FROM People
                WHERE LOWER(FullName) LIKE ?
                   OR LOWER(NVL(Nickname, '')) LIKE ?
                   OR LOWER(Nationality) LIKE ?
                ORDER BY FullName
                """;
        String value = like(search);
        return queryTable(
                new String[]{"PersonID", "Nickname", "Name", "Birthday", "Nationality"},
                sql,
                statement -> {
                    statement.setString(1, value);
                    statement.setString(2, value);
                    statement.setString(3, value);
                }
        );
    }

    public TableData findTeamsBasic(String search) throws SQLException {
        String sql = """
                SELECT TeamName, Region
                FROM Team
                WHERE LOWER(TeamName) LIKE ?
                   OR LOWER(Region) LIKE ?
                ORDER BY TeamName
                """;
        String value = like(search);
        return queryTable(
                new String[]{"Team Name", "Region"},
                sql,
                statement -> {
                    statement.setString(1, value);
                    statement.setString(2, value);
                }
        );
    }

    public TableData findTournamentsBasic(String search) throws SQLException {
        String sql = """
                SELECT TournamentName, TournyStartDate, TournyEndDate
                FROM Tournament
                WHERE LOWER(TournamentName) LIKE ?
                ORDER BY TournyStartDate DESC NULLS LAST, TournamentID DESC
                """;
        return queryTable(
                new String[]{"Tournament Name", "Start Date", "End Date"},
                sql,
                statement -> statement.setString(1, like(search))
        );
    }

    public void addTeam(String teamName, String region) throws SQLException {
        String sql = """
                INSERT INTO Team (TeamName, Region)
                VALUES (?, ?)
                """;
        String cleanTeamName = requiredText(teamName, "Team name", 100);
        String cleanRegion = requiredText(region, "Region", 50);
        executeUpdate(sql, statement -> {
            statement.setString(1, cleanTeamName);
            statement.setString(2, cleanRegion);
        });
    }

    public void updateTeamRegion(String teamName, String region) throws SQLException {
        String sql = """
                UPDATE Team
                SET Region = ?
                WHERE TeamName = ?
                """;
        String cleanTeamName = requiredText(teamName, "Team name", 100);
        String cleanRegion = requiredText(region, "Region", 50);
        int rowsUpdated = executeUpdate(sql, statement -> {
            statement.setString(1, cleanRegion);
            statement.setString(2, cleanTeamName);
        });
        if (rowsUpdated == 0) {
            throw new IllegalArgumentException("Team was not found: " + cleanTeamName);
        }
    }

    // ── Generic, metadata-driven CRUD ──────────────────────────────────────

    /** Loads every column of a table, optionally filtered by a free-text search across all columns. */
    public TableData browse(TableSpec spec, String search) throws SQLException {
        List<ColumnSpec> columns = spec.columns();
        StringJoiner select = new StringJoiner(", ");
        for (ColumnSpec column : columns) {
            select.add(column.name());
        }

        StringBuilder sql = new StringBuilder("SELECT ").append(select)
                .append(" FROM ").append(spec.tableName());

        String trimmed = normalize(search);
        boolean filtered = !trimmed.isEmpty();
        if (filtered) {
            StringJoiner where = new StringJoiner(" OR ", " WHERE ", "");
            for (ColumnSpec column : columns) {
                if (column.isDate()) {
                    where.add("TO_CHAR(" + column.name() + ", 'YYYY-MM-DD') LIKE ?");
                } else {
                    where.add("LOWER(TO_CHAR(" + column.name() + ")) LIKE ?");
                }
            }
            sql.append(where);
        }
        sql.append(" ORDER BY ").append(spec.orderBy());

        String textLike = like(trimmed);
        String dateLike = numberLike(trimmed);
        return queryTable(spec.columnLabels(), sql.toString(), statement -> {
            if (filtered) {
                int index = 1;
                for (ColumnSpec column : columns) {
                    statement.setString(index++, column.isDate() ? dateLike : textLike);
                }
            }
        });
    }

    /** Distinct, non-null values of a column — used to populate foreign-key dropdowns. */
    public List<String> distinctValues(String table, String column) throws SQLException {
        String sql = "SELECT DISTINCT " + column + " FROM " + table
                + " WHERE " + column + " IS NOT NULL ORDER BY " + column;
        List<String> values = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                values.add(String.valueOf(resultSet.getObject(1)));
            }
        }
        return values;
    }

    public void insertRow(TableSpec spec, Map<String, String> rawValues) throws SQLException {
        List<ColumnSpec> columns = spec.insertableColumns();
        StringJoiner names = new StringJoiner(", ");
        StringJoiner marks = new StringJoiner(", ");
        for (ColumnSpec column : columns) {
            names.add(column.name());
            marks.add("?");
        }
        String sql = "INSERT INTO " + spec.tableName() + " (" + names + ") VALUES (" + marks + ")";
        executeWrite(sql, statement -> {
            int index = 1;
            for (ColumnSpec column : columns) {
                bind(statement, index++, column, rawValues.get(column.name()));
            }
        });
    }

    public void updateRow(TableSpec spec, Map<String, String> primaryKey, Map<String, String> rawValues) throws SQLException {
        List<ColumnSpec> editable = spec.editableColumns();
        if (editable.isEmpty()) {
            throw new IllegalArgumentException("This table has no editable (non-key) columns.");
        }
        StringJoiner set = new StringJoiner(", ");
        for (ColumnSpec column : editable) {
            set.add(column.name() + " = ?");
        }
        List<ColumnSpec> keys = spec.primaryKey();
        StringJoiner where = new StringJoiner(" AND ");
        for (ColumnSpec key : keys) {
            where.add(key.name() + " = ?");
        }
        String sql = "UPDATE " + spec.tableName() + " SET " + set + " WHERE " + where;
        int affected = executeWrite(sql, statement -> {
            int index = 1;
            for (ColumnSpec column : editable) {
                bind(statement, index++, column, rawValues.get(column.name()));
            }
            for (ColumnSpec key : keys) {
                bind(statement, index++, key, primaryKey.get(key.name()));
            }
        });
        if (affected == 0) {
            throw new IllegalArgumentException("No matching row was found to update.");
        }
    }

    public void deleteRow(TableSpec spec, Map<String, String> primaryKey) throws SQLException {
        List<ColumnSpec> keys = spec.primaryKey();
        StringJoiner where = new StringJoiner(" AND ");
        for (ColumnSpec key : keys) {
            where.add(key.name() + " = ?");
        }
        String sql = "DELETE FROM " + spec.tableName() + " WHERE " + where;
        int affected = executeWrite(sql, statement -> {
            int index = 1;
            for (ColumnSpec key : keys) {
                bind(statement, index++, key, primaryKey.get(key.name()));
            }
        });
        if (affected == 0) {
            throw new IllegalArgumentException("No matching row was found to delete.");
        }
    }

    // ── Preset analytical queries ───────────────────────────────────────────

    public TableData runAnalytics(AnalyticsQuery query, String parameter) throws SQLException {
        String value = like(parameter);
        int placeholders = query.placeholderCount();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(query.sql())) {
            for (int index = 1; index <= placeholders; index++) {
                statement.setString(index, value);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return readResultSet(resultSet);
            }
        }
    }

    private TableData readResultSet(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        String[] columns = new String[columnCount];
        for (int index = 0; index < columnCount; index++) {
            columns[index] = metaData.getColumnLabel(index + 1).replace('_', ' ');
        }
        List<Object[]> rows = new ArrayList<>();
        while (resultSet.next()) {
            Object[] row = new Object[columnCount];
            for (int index = 0; index < columnCount; index++) {
                row[index] = safeGet(resultSet, metaData, index + 1);
            }
            rows.add(row);
        }
        return new TableData(columns, rows);
    }

    public HomeSummary loadHomeSummary() throws SQLException {
        TableData topPlayers = findTopPlayers("");
        TableData trendingTeams = findTrendingTeams("");

        String playerName = "No players";
        String playerDetail = "";
        if (!topPlayers.rows().isEmpty()) {
            Object[] row = topPlayers.rows().get(0);
            playerName = row[1] + " (" + row[2] + ")";
            playerDetail = "Rating " + row[3] + " | ADR " + row[4] + " | DPR " + row[5];
        }

        String teamName = "No teams";
        String teamDetail = "";
        if (!trendingTeams.rows().isEmpty()) {
            Object[] row = trendingTeams.rows().get(0);
            teamName = String.valueOf(row[0]);
            teamDetail = row[1] + " | Wins " + row[3] + " | Recent matches " + row[2];
        }

        return new HomeSummary(
                countPlayers(),
                countTeams(),
                countMatches(),
                countTournaments(),
                playerName,
                playerDetail,
                teamName,
                teamDetail
        );
    }

    private int countPlayers() throws SQLException {
        return count("SELECT COUNT(*) FROM Player");
    }

    private int countTeams() throws SQLException {
        return count("SELECT COUNT(*) FROM Team");
    }

    private int countMatches() throws SQLException {
        return count("SELECT COUNT(*) FROM MatchTable");
    }

    private int countTournaments() throws SQLException {
        return count("SELECT COUNT(*) FROM Tournament");
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private TableData queryTable(String[] columns, String sql, StatementBinder binder) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                while (resultSet.next()) {
                    Object[] row = new Object[columns.length];
                    for (int index = 0; index < columns.length; index++) {
                        row[index] = safeGet(resultSet, metaData, index + 1);
                    }
                    rows.add(row);
                }
            }
        }
        return new TableData(columns, rows);
    }

    /**
     * Reads a single cell, forcing DATE/TIMESTAMP columns to {@code java.sql.Timestamp}
     * so they are never returned as Oracle-specific types whose toString() may use
     * the container's NLS locale (e.g. Chinese month names).
     */
    private static Object safeGet(ResultSet rs, ResultSetMetaData meta, int col)
            throws SQLException {
        int type = meta.getColumnType(col);
        if (type == java.sql.Types.DATE || type == java.sql.Types.TIMESTAMP
                || type == java.sql.Types.TIMESTAMP_WITH_TIMEZONE) {
            return rs.getTimestamp(col); // always java.sql.Timestamp, a java.util.Date
        }
        return rs.getObject(col);
    }

    private int executeUpdate(String sql, StatementBinder binder) throws SQLException {
        return executeWrite(sql, binder);
    }

    /** Runs a write statement inside an explicit transaction, rolling back on failure. */
    private int executeWrite(String sql, StatementBinder binder) throws SQLException {
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                binder.bind(statement);
                int affected = statement.executeUpdate();
                connection.commit();
                return affected;
            } catch (SQLException exception) {
                safeRollback(connection);
                throw new SQLException(friendlyMessage(exception), exception);
            } catch (RuntimeException exception) {
                safeRollback(connection);
                throw exception;
            }
        }
    }

    private static void safeRollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Nothing more we can do; the original failure is already being reported.
        }
    }

    /** Turns common Oracle constraint errors into actionable, human-readable messages. */
    private static String friendlyMessage(SQLException exception) {
        int code = exception.getErrorCode();
        return switch (code) {
            case 1 -> "That record already exists (a primary key or unique value is duplicated).";
            case 2291 -> "Referenced record does not exist. Create the parent record first (foreign-key check failed).";
            case 2292 -> "Cannot delete: other records still reference this row. Remove the dependent records first.";
            case 1400 -> "A required field was left empty.";
            case 1438, 12899 -> "A value is too large for its column.";
            default -> exception.getMessage();
        };
    }

    /** Converts the raw text from a form field into a typed JDBC value and binds it. */
    private static void bind(PreparedStatement statement, int index, ColumnSpec column, String raw) throws SQLException {
        String value = normalize(raw);
        if (value.isEmpty()) {
            if (column.required()) {
                throw new IllegalArgumentException(column.label() + " is required.");
            }
            statement.setNull(index, sqlType(column.type()));
            return;
        }
        switch (column.type()) {
            case TEXT -> statement.setString(index, value);
            case INTEGER -> {
                try {
                    statement.setLong(index, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(column.label() + " must be a whole number.");
                }
            }
            case DECIMAL -> {
                try {
                    statement.setBigDecimal(index, new BigDecimal(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(column.label() + " must be a number.");
                }
            }
            case DATE -> {
                try {
                    statement.setDate(index, java.sql.Date.valueOf(LocalDate.parse(value)));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(column.label() + " must be a date in YYYY-MM-DD format.");
                }
            }
        }
    }

    private static int sqlType(ColumnSpec.Type type) {
        return switch (type) {
            case TEXT -> Types.VARCHAR;
            case INTEGER -> Types.INTEGER;
            case DECIMAL -> Types.NUMERIC;
            case DATE -> Types.DATE;
        };
    }

    private static String like(String search) {
        return "%" + normalize(search).toLowerCase() + "%";
    }

    private static String numberLike(String search) {
        return "%" + normalize(search) + "%";
    }

    private static String normalize(String search) {
        return search == null ? "" : search.trim();
    }

    private static String requiredText(String value, String fieldName, int maxLength) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or fewer.");
        }
        return normalized;
    }

    @Override
    public boolean supportsAnalytics() {
        return true;
    }

    @Override
    public String dbLabel() {
        return "Oracle";
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
