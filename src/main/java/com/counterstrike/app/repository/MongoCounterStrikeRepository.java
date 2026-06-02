package com.counterstrike.app.repository;

import com.counterstrike.app.db.MongoDatabase;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MongoCounterStrikeRepository implements AppRepository {

    private final MongoDatabase mongoDatabase;

    public MongoCounterStrikeRepository(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    // ── Navigation queries ──────────────────────────────────────────────────

    @Override
    public TableData findTopPlayers(String search) {
        String[] cols = {"PersonID", "Nickname", "Name", "Rating", "ADR", "DPR"};
        Map<Object, Document> playerMap = buildMap(col("player"), "PersonID");
        List<Object[]> rows = new ArrayList<>();
        for (Document person : col("people").find()) {
            Document player = playerMap.get(person.get("PersonID"));
            if (player == null) continue;
            String nick = str(person, "Nickname");
            String name = str(person, "FullName");
            if (!hits(search, nick, name)) continue;
            rows.add(new Object[]{
                person.get("PersonID"), nick, name,
                player.get("Rating"), player.get("ADR"), player.get("DPR")
            });
        }
        rows.sort((a, b) -> descNum(a[3], b[3]));
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTrendingTeams(String search) {
        String[] cols = {"Team Name", "Region", "Recent Matches", "Wins"};
        List<Document> allMatches = drain(col("matchtable").find());
        List<Object[]> rows = new ArrayList<>();
        for (Document team : col("team").find()) {
            String teamName = str(team, "TeamName");
            if (!hits(search, teamName, str(team, "Region"))) continue;
            long matches = allMatches.stream()
                    .filter(m -> teamName.equals(str(m, "TeamA")) || teamName.equals(str(m, "TeamB")))
                    .count();
            long wins = allMatches.stream()
                    .filter(m -> teamName.equals(str(m, "WinnerTeam")))
                    .count();
            rows.add(new Object[]{teamName, str(team, "Region"), matches, wins});
        }
        rows.sort((a, b) -> Long.compare((Long) b[3], (Long) a[3]));
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTeamMatchHistory(String search) {
        String[] cols = {"Match", "Score", "Tournament", "Date", "Winner", "Stage"};
        Map<Object, Document> tourneyMap = buildMap(col("tournament"), "TournamentID");
        List<Object[]> rows = new ArrayList<>();
        for (Document m : col("matchtable").find()) {
            String teamA = str(m, "TeamA");
            String teamB = str(m, "TeamB");
            String winner = str(m, "WinnerTeam");
            String stage = str(m, "Stage");
            Document tourney = tourneyMap.get(m.get("TournamentID"));
            String tourneyName = tourney == null ? "" : str(tourney, "TournamentName");
            if (!hits(search, teamA, teamB, winner, stage, tourneyName)) continue;
            String score = nvl(str(m, "MatchResultTeamA"), "-") + "-" + nvl(str(m, "MatchResultTeamB"), "-");
            rows.add(new Object[]{teamA + " vs " + teamB, score, tourneyName,
                fmt(m.get("MatchDate")), winner, stage});
        }
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTournamentMatchSummary(String search) {
        String[] cols = {"Tournament Name", "Start Date", "End Date", "Match Count"};
        List<Document> allMatches = drain(col("matchtable").find());
        List<Object[]> rows = new ArrayList<>();
        for (Document t : col("tournament").find()) {
            String name = str(t, "TournamentName");
            if (!hits(search, name)) continue;
            Object tid = t.get("TournamentID");
            long count = allMatches.stream()
                    .filter(m -> Objects.equals(m.get("TournamentID"), tid))
                    .count();
            rows.add(new Object[]{name, fmt(t.get("TournyStartDate")), fmt(t.get("TournyEndDate")), count});
        }
        return new TableData(cols, rows);
    }

    @Override
    public TableData findPeople(String search) {
        String[] cols = {"PersonID", "Nickname", "Name", "Birthday", "Nationality"};
        List<Object[]> rows = new ArrayList<>();
        for (Document p : col("people").find()) {
            String nick = str(p, "Nickname");
            String name = str(p, "FullName");
            String nat = str(p, "Nationality");
            if (!hits(search, nick, name, nat)) continue;
            rows.add(new Object[]{p.get("PersonID"), nick, name, fmt(p.get("Birthday")), nat});
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r[2])));
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTeamsBasic(String search) {
        String[] cols = {"Team Name", "Region"};
        List<Object[]> rows = new ArrayList<>();
        for (Document t : col("team").find()) {
            String name = str(t, "TeamName");
            String region = str(t, "Region");
            if (!hits(search, name, region)) continue;
            rows.add(new Object[]{name, region});
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r[0])));
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTournamentsBasic(String search) {
        String[] cols = {"Tournament Name", "Start Date", "End Date"};
        List<Object[]> rows = new ArrayList<>();
        for (Document t : col("tournament").find()) {
            String name = str(t, "TournamentName");
            if (!hits(search, name)) continue;
            rows.add(new Object[]{name, fmt(t.get("TournyStartDate")), fmt(t.get("TournyEndDate"))});
        }
        return new TableData(cols, rows);
    }

    // ── Write operations ────────────────────────────────────────────────────

    @Override
    public void addTeam(String teamName, String region) {
        requireField(teamName, "Team name", 100);
        requireField(region, "Region", 50);
        if (col("team").countDocuments(Filters.eq("TeamName", teamName)) > 0) {
            throw new IllegalArgumentException("Team already exists: " + teamName);
        }
        col("team").insertOne(new Document("TeamName", teamName).append("Region", region));
    }

    @Override
    public void updateTeamRegion(String teamName, String region) {
        requireField(teamName, "Team name", 100);
        requireField(region, "Region", 50);
        var result = col("team").updateOne(
                Filters.eq("TeamName", teamName),
                Updates.set("Region", region));
        if (result.getMatchedCount() == 0) {
            throw new IllegalArgumentException("Team not found: " + teamName);
        }
    }

    @Override
    public HomeSummary loadHomeSummary() {
        TableData players = findTopPlayers("");
        TableData teams = findTrendingTeams("");
        String playerName = "No players", playerDetail = "";
        if (!players.rows().isEmpty()) {
            Object[] r = players.rows().get(0);
            playerName = r[1] + " (" + r[2] + ")";
            playerDetail = "Rating " + r[3] + " | ADR " + r[4] + " | DPR " + r[5];
        }
        String teamName = "No teams", teamDetail = "";
        if (!teams.rows().isEmpty()) {
            Object[] r = teams.rows().get(0);
            teamName = String.valueOf(r[0]);
            teamDetail = r[1] + " | Wins " + r[3] + " | Recent matches " + r[2];
        }
        return new HomeSummary(
                (int) col("player").countDocuments(),
                (int) col("team").countDocuments(),
                (int) col("matchtable").countDocuments(),
                (int) col("tournament").countDocuments(),
                playerName, playerDetail, teamName, teamDetail);
    }

    // ── Generic CRUD ────────────────────────────────────────────────────────

    @Override
    public TableData browse(TableSpec spec, String search) {
        List<ColumnSpec> columns = spec.columns();
        String[] headers = spec.columnLabels();
        String colName = spec.tableName().toLowerCase();
        String trimmed = search == null ? "" : search.trim().toLowerCase();
        List<Object[]> rows = new ArrayList<>();
        for (Document doc : col(colName).find()) {
            if (!trimmed.isEmpty()) {
                boolean match = false;
                for (ColumnSpec column : columns) {
                    Object val = doc.get(column.name());
                    if (val != null && val.toString().toLowerCase().contains(trimmed)) {
                        match = true;
                        break;
                    }
                }
                if (!match) continue;
            }
            Object[] row = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                row[i] = columns.get(i).isDate()
                        ? fmt(doc.get(columns.get(i).name()))
                        : doc.get(columns.get(i).name());
            }
            rows.add(row);
        }
        return new TableData(headers, rows);
    }

    @Override
    public List<String> distinctValues(String table, String column) {
        List<String> result = new ArrayList<>();
        for (Object val : col(table.toLowerCase()).distinct(column, Object.class)) {
            if (val != null) result.add(val.toString());
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public void insertRow(TableSpec spec, Map<String, String> rawValues) {
        Document doc = new Document();
        for (ColumnSpec column : spec.insertableColumns()) {
            String raw = rawValues.get(column.name());
            if (raw == null || raw.isBlank()) {
                if (column.required()) {
                    throw new IllegalArgumentException(column.label() + " is required.");
                }
            } else {
                doc.append(column.name(), toValue(column, raw));
            }
        }
        try {
            col(spec.tableName().toLowerCase()).insertOne(doc);
        } catch (MongoException e) {
            throw new IllegalStateException(friendlyMongoError(e), e);
        }
    }

    @Override
    public void updateRow(TableSpec spec, Map<String, String> primaryKey, Map<String, String> rawValues) {
        Bson filter = pkFilter(spec, primaryKey);
        List<Bson> updates = new ArrayList<>();
        for (ColumnSpec column : spec.editableColumns()) {
            String raw = rawValues.get(column.name());
            if (raw != null && !raw.isBlank()) {
                updates.add(Updates.set(column.name(), toValue(column, raw)));
            } else if (!column.required()) {
                updates.add(Updates.unset(column.name()));
            }
        }
        if (updates.isEmpty()) throw new IllegalArgumentException("Nothing to update.");
        try {
            var result = col(spec.tableName().toLowerCase()).updateOne(filter, Updates.combine(updates));
            if (result.getMatchedCount() == 0) {
                throw new IllegalArgumentException("No matching row was found to update.");
            }
        } catch (MongoException e) {
            throw new IllegalStateException(friendlyMongoError(e), e);
        }
    }

    @Override
    public void deleteRow(TableSpec spec, Map<String, String> primaryKey) {
        Bson filter = pkFilter(spec, primaryKey);
        try {
            var result = col(spec.tableName().toLowerCase()).deleteOne(filter);
            if (result.getDeletedCount() == 0) {
                throw new IllegalArgumentException("No matching row was found to delete.");
            }
        } catch (MongoException e) {
            throw new IllegalStateException(friendlyMongoError(e), e);
        }
    }

    // ── Analytics ──────────────────────────────────────────────────────────

    @Override
    public TableData runAnalytics(AnalyticsQuery query, String parameter) {
        throw new UnsupportedOperationException(
                "Advanced SQL queries are not available in MongoDB mode.\nSwitch to Oracle to use the Analytics tab.");
    }

    @Override
    public boolean supportsAnalytics() {
        return false;
    }

    @Override
    public String dbLabel() {
        return "MongoDB";
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private MongoCollection<Document> col(String name) {
        return mongoDatabase.getDatabase().getCollection(name);
    }

    private static Map<Object, Document> buildMap(MongoCollection<Document> collection, String keyField) {
        Map<Object, Document> map = new HashMap<>();
        for (Document doc : collection.find()) {
            map.put(doc.get(keyField), doc);
        }
        return map;
    }

    private static List<Document> drain(Iterable<Document> iterable) {
        List<Document> list = new ArrayList<>();
        for (Document doc : iterable) list.add(doc);
        return list;
    }

    private static String str(Document doc, String key) {
        Object val = doc.get(key);
        return val == null ? "" : val.toString();
    }

    private static String nvl(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static boolean hits(String search, String... values) {
        if (search == null || search.isBlank()) return true;
        String lower = search.trim().toLowerCase();
        for (String v : values) {
            if (v != null && v.toLowerCase().contains(lower)) return true;
        }
        return false;
    }

    private static int descNum(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(nb.doubleValue(), na.doubleValue());
        }
        return b.toString().compareTo(a.toString());
    }

    private static String fmt(Object val) {
        if (val == null) return null;
        if (val instanceof java.util.Date date) {
            return new SimpleDateFormat("yyyy-MM-dd").format(date);
        }
        return val.toString();
    }

    private static Object toValue(ColumnSpec column, String raw) {
        String trimmed = raw.trim();
        return switch (column.type()) {
            case TEXT -> trimmed;
            case INTEGER -> {
                try { yield Long.parseLong(trimmed); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(column.label() + " must be a whole number.");
                }
            }
            case DECIMAL -> {
                try { yield Double.parseDouble(trimmed); }
                catch (NumberFormatException e) {
                    throw new IllegalArgumentException(column.label() + " must be a number.");
                }
            }
            case DATE -> {
                try {
                    LocalDate date = LocalDate.parse(trimmed);
                    yield new java.util.Date(date.toEpochDay() * 86_400_000L);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(column.label() + " must be a date in YYYY-MM-DD format.");
                }
            }
        };
    }

    private static Bson pkFilter(TableSpec spec, Map<String, String> primaryKey) {
        List<ColumnSpec> keys = spec.primaryKey();
        if (keys.size() == 1) {
            ColumnSpec key = keys.get(0);
            return Filters.eq(key.name(), toValue(key, primaryKey.get(key.name())));
        }
        List<Bson> conditions = new ArrayList<>();
        for (ColumnSpec key : keys) {
            conditions.add(Filters.eq(key.name(), toValue(key, primaryKey.get(key.name()))));
        }
        return Filters.and(conditions);
    }

    private static void requireField(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or fewer.");
        }
    }

    private static String friendlyMongoError(MongoException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("duplicate key")) {
            return "That record already exists (duplicate key).";
        }
        return msg;
    }
}
