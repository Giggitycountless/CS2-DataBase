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
import java.util.*;

/**
 * MongoDB implementation of AppRepository targeting the cs_small database,
 * which uses an embedded document design:
 *   teams       – { _id: "TeamName", region }
 *   people      – { _id: personId, nickname, fullName, birthday, nationality,
 *                   type("player"|"coach"), playerStats{rating,adr,dpr},
 *                   coachStats{winRate}, contract{teamName,startDate,endDate,inGameRole} }
 *   tournaments – { _id: tournamentId, tournamentName, startDate, endDate,
 *                   participation:[{teamName,placement}] }
 *   matches     – { _id: matchId, tournamentId, matchDate, stage,
 *                   teamA, teamB, results, winnerTeam, winningCondition,
 *                   records:[{matchRecordId, map, ..., playerStats:[...]}] }
 */
public final class MongoCounterStrikeRepository implements AppRepository {

    private final MongoDatabase mongoDatabase;

    public MongoCounterStrikeRepository(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    // ── Navigation queries ──────────────────────────────────────────────────

    @Override
    public TableData findTopPlayers(String search) {
        String[] cols = {"PersonID", "Nickname", "Name", "Rating", "ADR", "DPR"};
        List<Object[]> rows = new ArrayList<>();
        for (Document p : col("people").find(Filters.eq("type", "player"))) {
            String nick = str(p, "nickname");
            String name = str(p, "fullName");
            if (!hits(search, nick, name)) continue;
            Document stats = doc(p, "playerStats");
            rows.add(new Object[]{
                p.get("_id"), nick, name,
                stats == null ? null : stats.get("rating"),
                stats == null ? null : stats.get("adr"),
                stats == null ? null : stats.get("dpr")
            });
        }
        rows.sort((a, b) -> descNum(a[3], b[3]));
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTrendingTeams(String search) {
        String[] cols = {"Team Name", "Region", "Recent Matches", "Wins"};
        List<Document> allMatches = drain(col("matches").find());
        List<Object[]> rows = new ArrayList<>();
        for (Document team : col("teams").find()) {
            String teamName = str(team, "_id");
            if (!hits(search, teamName, str(team, "region"))) continue;
            long matches = allMatches.stream()
                    .filter(m -> teamName.equals(str(m, "teamA")) || teamName.equals(str(m, "teamB")))
                    .count();
            long wins = allMatches.stream()
                    .filter(m -> teamName.equals(str(m, "winnerTeam")))
                    .count();
            rows.add(new Object[]{teamName, str(team, "region"), matches, wins});
        }
        rows.sort((a, b) -> Long.compare((Long) b[3], (Long) a[3]));
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTeamMatchHistory(String search) {
        String[] cols = {"Match", "Score", "Tournament", "Date", "Winner", "Stage"};
        Map<Object, Document> tourneyMap = buildMap(col("tournaments"), "_id");
        List<Object[]> rows = new ArrayList<>();
        for (Document m : col("matches").find()) {
            String teamA = str(m, "teamA");
            String teamB = str(m, "teamB");
            String winner = str(m, "winnerTeam");
            String stage = str(m, "stage");
            Document tourney = tourneyMap.get(m.get("tournamentId"));
            String tourneyName = tourney == null ? "" : str(tourney, "tournamentName");
            if (!hits(search, teamA, teamB, winner, stage, tourneyName)) continue;
            String score = nvl(str(m, "matchResultTeamA"), "-") + "-" + nvl(str(m, "matchResultTeamB"), "-");
            rows.add(new Object[]{teamA + " vs " + teamB, score, tourneyName, fmt(m.get("matchDate")), winner, stage});
        }
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTournamentMatchSummary(String search) {
        String[] cols = {"Tournament Name", "Start Date", "End Date", "Match Count"};
        List<Document> allMatches = drain(col("matches").find());
        List<Object[]> rows = new ArrayList<>();
        for (Document t : col("tournaments").find()) {
            String name = str(t, "tournamentName");
            if (!hits(search, name)) continue;
            Object tid = t.get("_id");
            long count = allMatches.stream()
                    .filter(m -> Objects.equals(m.get("tournamentId"), tid))
                    .count();
            rows.add(new Object[]{name, fmt(t.get("startDate")), fmt(t.get("endDate")), count});
        }
        return new TableData(cols, rows);
    }

    @Override
    public TableData findPeople(String search) {
        String[] cols = {"PersonID", "Nickname", "Name", "Birthday", "Nationality"};
        List<Object[]> rows = new ArrayList<>();
        for (Document p : col("people").find()) {
            String nick = str(p, "nickname");
            String name = str(p, "fullName");
            String nat = str(p, "nationality");
            if (!hits(search, nick, name, nat)) continue;
            rows.add(new Object[]{p.get("_id"), nick, name, fmt(p.get("birthday")), nat});
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r[2])));
        return new TableData(cols, rows);
    }

    @Override
    public TableData findTeamsBasic(String search) {
        String[] cols = {"Team Name", "Region"};
        List<Object[]> rows = new ArrayList<>();
        for (Document t : col("teams").find()) {
            String name = str(t, "_id");
            String region = str(t, "region");
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
        for (Document t : col("tournaments").find()) {
            String name = str(t, "tournamentName");
            if (!hits(search, name)) continue;
            rows.add(new Object[]{name, fmt(t.get("startDate")), fmt(t.get("endDate"))});
        }
        return new TableData(cols, rows);
    }

    // ── Write operations ────────────────────────────────────────────────────

    @Override
    public void addTeam(String teamName, String region) {
        requireField(teamName, "Team name", 100);
        requireField(region, "Region", 50);
        if (col("teams").countDocuments(Filters.eq("_id", teamName)) > 0) {
            throw new IllegalArgumentException("Team already exists: " + teamName);
        }
        col("teams").insertOne(new Document("_id", teamName).append("region", region));
    }

    @Override
    public void updateTeamRegion(String teamName, String region) {
        requireField(teamName, "Team name", 100);
        requireField(region, "Region", 50);
        var result = col("teams").updateOne(Filters.eq("_id", teamName), Updates.set("region", region));
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
                (int) col("people").countDocuments(Filters.eq("type", "player")),
                (int) col("teams").countDocuments(),
                (int) col("matches").countDocuments(),
                (int) col("tournaments").countDocuments(),
                playerName, playerDetail, teamName, teamDetail);
    }

    // ── Generic CRUD ────────────────────────────────────────────────────────

    @Override
    public TableData browse(TableSpec spec, String search) {
        String table = spec.tableName();
        List<ColumnSpec> columns = spec.columns();
        String[] headers = spec.columnLabels();
        String trimmed = search == null ? "" : search.trim().toLowerCase();
        List<Object[]> rows = new ArrayList<>();

        Iterable<Document> docs = docsFor(table);
        for (Document docRaw : docs) {
            // For embedded tables, docRaw may be a sub-document with a parent reference
            Object[] row = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                row[i] = extractField(docRaw, columns.get(i).name(), table);
            }
            if (!trimmed.isEmpty()) {
                boolean match = false;
                for (Object v : row) {
                    if (v != null && v.toString().toLowerCase().contains(trimmed)) {
                        match = true; break;
                    }
                }
                if (!match) continue;
            }
            rows.add(row);
        }
        return new TableData(headers, rows);
    }

    @Override
    public List<String> distinctValues(String table, String column) {
        // Map Oracle table/column references to MongoDB equivalents
        String mongoCol = mongoCollectionFor(table);
        String mongoField = mongoFieldFor(table, column);

        List<String> result = new ArrayList<>();
        for (Object val : col(mongoCol).distinct(mongoField, Object.class)) {
            if (val != null) result.add(fmtKey(val));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public void insertRow(TableSpec spec, Map<String, String> rawValues) {
        try {
            switch (spec.tableName()) {
                case "People" -> {
                    long id = requireLong(rawValues.get("PersonID"), "Person ID");
                    col("people").insertOne(new Document("_id", id)
                            .append("nickname", rawValues.get("Nickname"))
                            .append("fullName", rawValues.get("FullName"))
                            .append("birthday", parseDate(rawValues.get("Birthday")))
                            .append("nationality", rawValues.get("Nationality"))
                            .append("type", "other"));
                }
                case "Player" -> {
                    // In cs_small, player stats are embedded inside the people document
                    long id = requireLong(rawValues.get("PersonID"), "Person ID");
                    Document stats = new Document("rating", parseDouble(rawValues.get("Rating")))
                            .append("adr", parseDouble(rawValues.get("ADR")))
                            .append("dpr", parseDouble(rawValues.get("DPR")));
                    var res = col("people").updateOne(Filters.eq("_id", id),
                            Updates.combine(Updates.set("type", "player"), Updates.set("playerStats", stats)));
                    if (res.getMatchedCount() == 0) {
                        throw new IllegalArgumentException(
                                "Person ID " + id + " not found. Add the person in the People table first.");
                    }
                }
                case "Coach" -> {
                    long id = requireLong(rawValues.get("PersonID"), "Person ID");
                    Document stats = new Document("winRate", parseDouble(rawValues.get("WinRate")));
                    var res = col("people").updateOne(Filters.eq("_id", id),
                            Updates.combine(Updates.set("type", "coach"), Updates.set("coachStats", stats)));
                    if (res.getMatchedCount() == 0) {
                        throw new IllegalArgumentException(
                                "Person ID " + id + " not found. Add the person in the People table first.");
                    }
                }
                case "Team" -> {
                    String name = rawValues.get("TeamName");
                    requireField(name, "Team Name", 100);
                    col("teams").insertOne(new Document("_id", name).append("region", rawValues.get("Region")));
                }
                case "PlayerContract" -> {
                    long id = requireLong(rawValues.get("PersonID"), "Person ID");
                    Document contract = new Document("teamName", rawValues.get("TeamName"))
                            .append("startDate", parseDate(rawValues.get("StartDate")))
                            .append("endDate", parseDate(rawValues.get("EndDate")))
                            .append("inGameRole", rawValues.get("InGameRole"));
                    col("people").updateOne(Filters.eq("_id", id), Updates.set("contract", contract));
                }
                case "CoachContract" -> {
                    long id = requireLong(rawValues.get("PersonID"), "Person ID");
                    Document contract = new Document("teamName", rawValues.get("TeamName"))
                            .append("startDate", parseDate(rawValues.get("StartDate")))
                            .append("endDate", parseDate(rawValues.get("EndDate")));
                    col("people").updateOne(Filters.eq("_id", id), Updates.set("contract", contract));
                }
                case "Tournament" -> {
                    long id = requireLong(rawValues.get("TournamentID"), "Tournament ID");
                    col("tournaments").insertOne(new Document("_id", id)
                            .append("tournamentName", rawValues.get("TournamentName"))
                            .append("startDate", parseDate(rawValues.get("TournyStartDate")))
                            .append("endDate", parseDate(rawValues.get("TournyEndDate")))
                            .append("participation", new ArrayList<>()));
                }
                case "MatchTable" -> {
                    long id = requireLong(rawValues.get("MatchID"), "Match ID");
                    col("matches").insertOne(new Document("_id", id)
                            .append("tournamentId", requireLong(rawValues.get("TournamentID"), "Tournament ID"))
                            .append("matchDate", parseDate(rawValues.get("MatchDate")))
                            .append("stage", rawValues.get("Stage"))
                            .append("teamA", rawValues.get("TeamA"))
                            .append("teamB", rawValues.get("TeamB"))
                            .append("matchResultTeamA", rawValues.get("MatchResultTeamA"))
                            .append("matchResultTeamB", rawValues.get("MatchResultTeamB"))
                            .append("winnerTeam", rawValues.get("WinnerTeam"))
                            .append("winningCondition", rawValues.get("WinningCondition"))
                            .append("records", new ArrayList<>()));
                }
                default -> throw new IllegalArgumentException(
                        spec.displayName() + " is read-only in MongoDB mode (data is embedded in parent documents).");
            }
        } catch (MongoException e) {
            throw new IllegalStateException(friendlyMongoError(e), e);
        }
    }

    @Override
    public void updateRow(TableSpec spec, Map<String, String> primaryKey, Map<String, String> rawValues) {
        try {
            switch (spec.tableName()) {
                case "People" -> {
                    long id = parseLongKey(primaryKey.get("PersonID"));
                    col("people").updateOne(Filters.eq("_id", id), Updates.combine(
                            Updates.set("nickname", rawValues.get("Nickname")),
                            Updates.set("fullName", rawValues.get("FullName")),
                            Updates.set("birthday", parseDate(rawValues.get("Birthday"))),
                            Updates.set("nationality", rawValues.get("Nationality"))));
                }
                case "Player" -> {
                    long id = parseLongKey(primaryKey.get("PersonID"));
                    Document stats = new Document("rating", parseDouble(rawValues.get("Rating")))
                            .append("adr", parseDouble(rawValues.get("ADR")))
                            .append("dpr", parseDouble(rawValues.get("DPR")));
                    col("people").updateOne(Filters.eq("_id", id), Updates.set("playerStats", stats));
                }
                case "Coach" -> {
                    long id = parseLongKey(primaryKey.get("PersonID"));
                    col("people").updateOne(Filters.eq("_id", id),
                            Updates.set("coachStats.winRate", parseDouble(rawValues.get("WinRate"))));
                }
                case "Team" -> {
                    String name = primaryKey.get("TeamName");
                    var res = col("teams").updateOne(Filters.eq("_id", name),
                            Updates.set("region", rawValues.get("Region")));
                    if (res.getMatchedCount() == 0) throw new IllegalArgumentException("Team not found: " + name);
                }
                case "PlayerContract" -> {
                    long id = parseLongKey(primaryKey.get("PersonID"));
                    Document contract = new Document("teamName", rawValues.get("TeamName"))
                            .append("startDate", parseDate(rawValues.get("StartDate")))
                            .append("endDate", parseDate(rawValues.get("EndDate")))
                            .append("inGameRole", rawValues.get("InGameRole"));
                    col("people").updateOne(Filters.eq("_id", id), Updates.set("contract", contract));
                }
                case "Tournament" -> {
                    long id = parseLongKey(primaryKey.get("TournamentID"));
                    col("tournaments").updateOne(Filters.eq("_id", id), Updates.combine(
                            Updates.set("tournamentName", rawValues.get("TournamentName")),
                            Updates.set("startDate", parseDate(rawValues.get("TournyStartDate"))),
                            Updates.set("endDate", parseDate(rawValues.get("TournyEndDate")))));
                }
                case "MatchTable" -> {
                    long id = parseLongKey(primaryKey.get("MatchID"));
                    col("matches").updateOne(Filters.eq("_id", id), Updates.combine(
                            Updates.set("stage", rawValues.get("Stage")),
                            Updates.set("teamA", rawValues.get("TeamA")),
                            Updates.set("teamB", rawValues.get("TeamB")),
                            Updates.set("matchResultTeamA", rawValues.get("MatchResultTeamA")),
                            Updates.set("matchResultTeamB", rawValues.get("MatchResultTeamB")),
                            Updates.set("winnerTeam", rawValues.get("WinnerTeam")),
                            Updates.set("winningCondition", rawValues.get("WinningCondition"))));
                }
                default -> throw new IllegalArgumentException(
                        spec.displayName() + " is read-only in MongoDB mode.");
            }
        } catch (MongoException e) {
            throw new IllegalStateException(friendlyMongoError(e), e);
        }
    }

    @Override
    public void deleteRow(TableSpec spec, Map<String, String> primaryKey) {
        try {
            switch (spec.tableName()) {
                case "People" -> {
                    long id = parseLongKey(primaryKey.get("PersonID"));
                    var res = col("people").deleteOne(Filters.eq("_id", id));
                    if (res.getDeletedCount() == 0) throw new IllegalArgumentException("Person not found.");
                }
                case "Player" -> {
                    long id = parseLongKey(primaryKey.get("PersonID"));
                    col("people").updateOne(Filters.eq("_id", id),
                            Updates.combine(Updates.unset("playerStats"), Updates.set("type", "other")));
                }
                case "Coach" -> {
                    long id = parseLongKey(primaryKey.get("PersonID"));
                    col("people").updateOne(Filters.eq("_id", id),
                            Updates.combine(Updates.unset("coachStats"), Updates.set("type", "other")));
                }
                case "Team" -> {
                    String name = primaryKey.get("TeamName");
                    var res = col("teams").deleteOne(Filters.eq("_id", name));
                    if (res.getDeletedCount() == 0) throw new IllegalArgumentException("Team not found: " + name);
                }
                case "PlayerContract", "CoachContract" -> {
                    long id = parseLongKey(primaryKey.get("PersonID"));
                    col("people").updateOne(Filters.eq("_id", id), Updates.unset("contract"));
                }
                case "Tournament" -> {
                    long id = parseLongKey(primaryKey.get("TournamentID"));
                    var res = col("tournaments").deleteOne(Filters.eq("_id", id));
                    if (res.getDeletedCount() == 0) throw new IllegalArgumentException("Tournament not found.");
                }
                case "MatchTable" -> {
                    long id = parseLongKey(primaryKey.get("MatchID"));
                    var res = col("matches").deleteOne(Filters.eq("_id", id));
                    if (res.getDeletedCount() == 0) throw new IllegalArgumentException("Match not found.");
                }
                default -> throw new IllegalArgumentException(
                        spec.displayName() + " is read-only in MongoDB mode.");
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

    // ── Internal helpers ────────────────────────────────────────────────────

    private MongoCollection<Document> col(String name) {
        return mongoDatabase.getDatabase().getCollection(name);
    }

    /** Returns an iterable of documents for the given Oracle table name, mapped to MongoDB. */
    private Iterable<Document> docsFor(String table) {
        return switch (table) {
            case "People" -> col("people").find();
            case "Player" -> col("people").find(Filters.eq("type", "player"));
            case "Coach"  -> col("people").find(Filters.eq("type", "coach"));
            case "Team"   -> col("teams").find();
            case "PlayerContract"  -> col("people").find(Filters.exists("contract"));
            case "CoachContract"   -> col("people").find(Filters.and(
                    Filters.eq("type", "coach"), Filters.exists("contract")));
            case "Tournament"      -> col("tournaments").find();
            case "MatchTable"      -> col("matches").find();
            case "MatchRecord"     -> expandRecords();
            case "PlayerMatchStats"-> expandPlayerStats();
            case "TournamentParticipation" -> expandParticipation();
            default -> col(table.toLowerCase()).find();
        };
    }

    /** Expands matches.records into flat documents with a parent MatchID field. */
    private Iterable<Document> expandRecords() {
        List<Document> result = new ArrayList<>();
        for (Document match : col("matches").find()) {
            Object matchId = match.get("_id");
            List<?> records = (List<?>) match.get("records");
            if (records == null) continue;
            for (Object r : records) {
                if (r instanceof Document rec) {
                    Document flat = new Document(rec).append("MatchID", matchId);
                    result.add(flat);
                }
            }
        }
        return result;
    }

    /** Expands matches.records[].playerStats into flat documents. */
    private Iterable<Document> expandPlayerStats() {
        List<Document> result = new ArrayList<>();
        for (Document match : col("matches").find()) {
            List<?> records = (List<?>) match.get("records");
            if (records == null) continue;
            for (Object r : records) {
                if (!(r instanceof Document rec)) continue;
                Object recId = rec.get("matchRecordId");
                List<?> stats = (List<?>) rec.get("playerStats");
                if (stats == null) continue;
                for (Object s : stats) {
                    if (s instanceof Document stat) {
                        result.add(new Document(stat).append("MatchRecordID", recId));
                    }
                }
            }
        }
        return result;
    }

    /** Expands tournaments.participation into flat documents. */
    private Iterable<Document> expandParticipation() {
        List<Document> result = new ArrayList<>();
        for (Document t : col("tournaments").find()) {
            Object tid = t.get("_id");
            List<?> parts = (List<?>) t.get("participation");
            if (parts == null) continue;
            for (Object p : parts) {
                if (p instanceof Document part) {
                    result.add(new Document(part).append("TournamentID", tid));
                }
            }
        }
        return result;
    }

    /**
     * Maps an Oracle column name (PascalCase) to the value from a MongoDB document
     * (camelCase / nested / _id based on the cs_small schema).
     */
    private Object extractField(Document doc, String colName, String table) {
        return switch (table + "." + colName) {
            // People
            case "People.PersonID"        -> doc.get("_id");
            case "People.Nickname"        -> doc.get("nickname");
            case "People.FullName"        -> doc.get("fullName");
            case "People.Birthday"        -> fmt(doc.get("birthday"));
            case "People.Nationality"     -> doc.get("nationality");
            // Player
            case "Player.PersonID"        -> doc.get("_id");
            case "Player.Rating"          -> nested(doc, "playerStats", "rating");
            case "Player.ADR"             -> nested(doc, "playerStats", "adr");
            case "Player.DPR"             -> nested(doc, "playerStats", "dpr");
            // Coach
            case "Coach.PersonID"         -> doc.get("_id");
            case "Coach.WinRate"          -> nested(doc, "coachStats", "winRate");
            // Team
            case "Team.TeamName"          -> doc.get("_id");
            case "Team.Region"            -> doc.get("region");
            // PlayerContract
            case "PlayerContract.PlayerContractID" -> doc.get("_id");
            case "PlayerContract.PersonID"         -> doc.get("_id");
            case "PlayerContract.TeamName"         -> nested(doc, "contract", "teamName");
            case "PlayerContract.StartDate"        -> fmt(nested(doc, "contract", "startDate"));
            case "PlayerContract.EndDate"          -> fmt(nested(doc, "contract", "endDate"));
            case "PlayerContract.InGameRole"       -> nested(doc, "contract", "inGameRole");
            // CoachContract
            case "CoachContract.CoachContractID"   -> doc.get("_id");
            case "CoachContract.PersonID"          -> doc.get("_id");
            case "CoachContract.TeamName"          -> nested(doc, "contract", "teamName");
            case "CoachContract.StartDate"         -> fmt(nested(doc, "contract", "startDate"));
            case "CoachContract.EndDate"           -> fmt(nested(doc, "contract", "endDate"));
            // Tournament
            case "Tournament.TournamentID"         -> doc.get("_id");
            case "Tournament.TournamentName"       -> doc.get("tournamentName");
            case "Tournament.TournyStartDate"      -> fmt(doc.get("startDate"));
            case "Tournament.TournyEndDate"        -> fmt(doc.get("endDate"));
            // MatchTable
            case "MatchTable.MatchID"              -> doc.get("_id");
            case "MatchTable.TournamentID"         -> doc.get("tournamentId");
            case "MatchTable.MatchDate"            -> fmt(doc.get("matchDate"));
            case "MatchTable.Stage"                -> doc.get("stage");
            case "MatchTable.TeamA"                -> doc.get("teamA");
            case "MatchTable.TeamB"                -> doc.get("teamB");
            case "MatchTable.MatchResultTeamA"     -> doc.get("matchResultTeamA");
            case "MatchTable.MatchResultTeamB"     -> doc.get("matchResultTeamB");
            case "MatchTable.WinnerTeam"           -> doc.get("winnerTeam");
            case "MatchTable.WinningCondition"     -> doc.get("winningCondition");
            // MatchRecord (expanded from records[])
            case "MatchRecord.MatchRecordID"       -> doc.get("matchRecordId");
            case "MatchRecord.MatchID"             -> doc.get("MatchID");
            case "MatchRecord.RecordDate"          -> fmt(doc.get("recordDate"));
            case "MatchRecord.TeamA"               -> doc.get("teamA");
            case "MatchRecord.TeamB"               -> doc.get("teamB");
            case "MatchRecord.StartingSide"        -> doc.get("startingSide");
            case "MatchRecord.FinalScore"          -> doc.get("finalScore");
            case "MatchRecord.TopHalfScore"        -> doc.get("topHalfScore");
            case "MatchRecord.BottomHalfScore"     -> doc.get("bottomHalfScore");
            case "MatchRecord.TeamAResult"         -> doc.get("teamAResult");
            case "MatchRecord.TeamBResult"         -> doc.get("teamBResult");
            case "MatchRecord.Map"                 -> doc.get("map");
            // PlayerMatchStats (expanded from records[].playerStats[])
            case "PlayerMatchStats.MatchRecordID"  -> doc.get("MatchRecordID");
            case "PlayerMatchStats.PlayerID"       -> doc.get("playerId");
            case "PlayerMatchStats.TeamName"       -> doc.get("teamName");
            case "PlayerMatchStats.Kills"          -> doc.get("kills");
            case "PlayerMatchStats.Deaths"         -> doc.get("deaths");
            case "PlayerMatchStats.Assists"        -> doc.get("assists");
            case "PlayerMatchStats.Rating"         -> doc.get("rating");
            case "PlayerMatchStats.ADR"            -> doc.get("adr");
            case "PlayerMatchStats.DPR"            -> doc.get("dpr");
            // TournamentParticipation (expanded from participation[])
            case "TournamentParticipation.TournamentID" -> doc.get("TournamentID");
            case "TournamentParticipation.TeamName"     -> doc.get("teamName");
            case "TournamentParticipation.Placement"    -> doc.get("placement");
            default -> doc.get(colName);
        };
    }

    /** Maps Oracle table name to MongoDB collection name. */
    private static String mongoCollectionFor(String table) {
        return switch (table) {
            case "People", "Player", "Coach", "PlayerContract", "CoachContract" -> "people";
            case "Team" -> "teams";
            case "Tournament", "TournamentParticipation" -> "tournaments";
            case "MatchTable", "MatchRecord", "PlayerMatchStats" -> "matches";
            default -> table.toLowerCase();
        };
    }

    /** Maps Oracle column name to MongoDB field for distinctValues(). */
    private static String mongoFieldFor(String table, String column) {
        return switch (table + "." + column) {
            case "People.PersonID"  -> "_id";
            case "Player.PersonID"  -> "_id";
            case "Coach.PersonID"   -> "_id";
            case "Team.TeamName"    -> "_id";
            case "Tournament.TournamentID" -> "_id";
            case "MatchTable.MatchID"      -> "_id";
            case "Team.Region"             -> "region";
            case "PlayerContract.TeamName", "CoachContract.TeamName" -> "contract.teamName";
            default -> column;
        };
    }

    private static Map<Object, Document> buildMap(MongoCollection<Document> collection, String keyField) {
        Map<Object, Document> map = new HashMap<>();
        for (Document d : collection.find()) {
            map.put(d.get(keyField), d);
        }
        return map;
    }

    private static List<Document> drain(Iterable<Document> iterable) {
        List<Document> list = new ArrayList<>();
        for (Document d : iterable) list.add(d);
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

    /** Format a MongoDB _id or numeric key without trailing ".0" */
    private static String fmtKey(Object val) {
        if (val instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf(d.longValue());
        }
        return val.toString();
    }

    private static Object nested(Document doc, String parent, String child) {
        Object p = doc.get(parent);
        if (p instanceof Document pd) return pd.get(child);
        return null;
    }

    private static Document doc(Document parent, String key) {
        Object val = parent.get(key);
        return val instanceof Document d ? d : null;
    }

    private static java.util.Date parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new java.util.Date(LocalDate.parse(raw.trim()).toEpochDay() * 86_400_000L);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must be YYYY-MM-DD format: " + raw);
        }
    }

    private static double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return 0.0;
        try { return Double.parseDouble(raw.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Must be a number: " + raw); }
    }

    /** Parses a long even if the value came back as "1.0" from MongoDB. */
    private static long parseLong(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Value is required.");
        try {
            String t = raw.trim();
            if (t.contains(".")) return (long) Double.parseDouble(t);
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Must be a whole number: " + raw);
        }
    }

    private static long parseLongKey(String raw) {
        return parseLong(raw);
    }

    private static long requireLong(String raw, String label) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return parseLong(raw);
    }

    private static void requireField(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(fieldName + " is required.");
        if (value.length() > maxLength)
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or fewer.");
    }

    private static String friendlyMongoError(MongoException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("duplicate key")) return "That record already exists (duplicate key).";
        return msg;
    }
}
