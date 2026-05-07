package com.counterstrike.app.repository;

import com.counterstrike.app.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class CounterStrikeRepository {
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
                while (resultSet.next()) {
                    Object[] row = new Object[columns.length];
                    for (int index = 0; index < columns.length; index++) {
                        row[index] = resultSet.getObject(index + 1);
                    }
                    rows.add(row);
                }
            }
        }
        return new TableData(columns, rows);
    }

    private int executeUpdate(String sql, StatementBinder binder) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        }
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

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
