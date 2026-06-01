package com.counterstrike.app.repository;

import com.counterstrike.app.repository.ColumnSpec.Type;

import java.util.List;

import static com.counterstrike.app.repository.ColumnSpec.col;
import static com.counterstrike.app.repository.ColumnSpec.pk;
import static com.counterstrike.app.repository.ColumnSpec.pkRef;
import static com.counterstrike.app.repository.ColumnSpec.ref;
import static com.counterstrike.app.repository.ColumnSpec.required;

/**
 * Central catalogue of every table in the CS2 schema, expressed as {@link TableSpec}
 * metadata. Adding a column here automatically flows through the generic CRUD UI.
 */
public final class Schema {

    public static final TableSpec PEOPLE = new TableSpec("People", "People", List.of(
            pk("PersonID", "Person ID", Type.INTEGER),
            required("Nickname", "Nickname", Type.TEXT),
            required("FullName", "Full Name", Type.TEXT),
            col("Birthday", "Birthday", Type.DATE),
            col("Nationality", "Nationality", Type.TEXT)
    ), "PersonID");

    public static final TableSpec PLAYER = new TableSpec("Player", "Player", List.of(
            pkRef("PersonID", "Person ID", Type.INTEGER, "People", "PersonID"),
            col("Rating", "Rating", Type.DECIMAL),
            col("ADR", "ADR", Type.DECIMAL),
            col("DPR", "DPR", Type.DECIMAL)
    ), "PersonID");

    public static final TableSpec COACH = new TableSpec("Coach", "Coach", List.of(
            pkRef("PersonID", "Person ID", Type.INTEGER, "People", "PersonID"),
            col("WinRate", "Win Rate", Type.DECIMAL)
    ), "PersonID");

    public static final TableSpec TEAM = new TableSpec("Team", "Team", List.of(
            pk("TeamName", "Team Name", Type.TEXT),
            required("Region", "Region", Type.TEXT)
    ), "TeamName");

    public static final TableSpec PLAYER_CONTRACT = new TableSpec("Player Contract", "PlayerContract", List.of(
            pk("PlayerContractID", "Contract ID", Type.INTEGER),
            ref("PersonID", "Player ID", Type.INTEGER, "Player", "PersonID", true),
            ref("TeamName", "Team", Type.TEXT, "Team", "TeamName", true),
            col("StartDate", "Start Date", Type.DATE),
            col("EndDate", "End Date", Type.DATE),
            col("InGameRole", "In-Game Role", Type.TEXT)
    ), "PlayerContractID");

    public static final TableSpec COACH_CONTRACT = new TableSpec("Coach Contract", "CoachContract", List.of(
            pk("CoachContractID", "Contract ID", Type.INTEGER),
            ref("PersonID", "Coach ID", Type.INTEGER, "Coach", "PersonID", true),
            ref("TeamName", "Team", Type.TEXT, "Team", "TeamName", true),
            col("StartDate", "Start Date", Type.DATE),
            col("EndDate", "End Date", Type.DATE)
    ), "CoachContractID");

    public static final TableSpec TOURNAMENT = new TableSpec("Tournament", "Tournament", List.of(
            pk("TournamentID", "Tournament ID", Type.INTEGER),
            required("TournamentName", "Tournament Name", Type.TEXT),
            col("TournyStartDate", "Start Date", Type.DATE),
            col("TournyEndDate", "End Date", Type.DATE)
    ), "TournyStartDate DESC NULLS LAST, TournamentID DESC");

    public static final TableSpec MATCH = new TableSpec("Match", "MatchTable", List.of(
            pk("MatchID", "Match ID", Type.INTEGER),
            ref("TournamentID", "Tournament", Type.INTEGER, "Tournament", "TournamentID", true),
            col("MatchDate", "Date", Type.DATE),
            col("Stage", "Stage", Type.TEXT),
            ref("TeamA", "Team A", Type.TEXT, "Team", "TeamName", true),
            ref("TeamB", "Team B", Type.TEXT, "Team", "TeamName", true),
            col("MatchResultTeamA", "Result A", Type.TEXT),
            col("MatchResultTeamB", "Result B", Type.TEXT),
            ref("WinnerTeam", "Winner", Type.TEXT, "Team", "TeamName", false),
            col("WinningCondition", "Winning Condition", Type.TEXT)
    ), "MatchDate DESC NULLS LAST, MatchID DESC");

    public static final TableSpec MATCH_RECORD = new TableSpec("Match Record", "MatchRecord", List.of(
            pk("MatchRecordID", "Record ID", Type.INTEGER),
            ref("MatchID", "Match", Type.INTEGER, "MatchTable", "MatchID", true),
            col("RecordDate", "Date", Type.DATE),
            ref("TeamA", "Team A", Type.TEXT, "Team", "TeamName", true),
            ref("TeamB", "Team B", Type.TEXT, "Team", "TeamName", true),
            col("StartingSide", "Starting Side", Type.TEXT),
            col("FinalScore", "Final Score", Type.TEXT),
            col("TopHalfScore", "Top Half", Type.TEXT),
            col("BottomHalfScore", "Bottom Half", Type.TEXT),
            col("TeamAResult", "Result A", Type.TEXT),
            col("TeamBResult", "Result B", Type.TEXT),
            col("Map", "Map", Type.TEXT)
    ), "MatchRecordID DESC");

    public static final TableSpec PLAYER_MATCH_STATS = new TableSpec("Player Match Stats", "PlayerMatchStats", List.of(
            pkRef("MatchRecordID", "Record ID", Type.INTEGER, "MatchRecord", "MatchRecordID"),
            pkRef("PlayerID", "Player ID", Type.INTEGER, "Player", "PersonID"),
            ref("TeamName", "Team", Type.TEXT, "Team", "TeamName", false),
            col("Kills", "Kills", Type.INTEGER),
            col("Deaths", "Deaths", Type.INTEGER),
            col("Assists", "Assists", Type.INTEGER),
            col("Rating", "Rating", Type.DECIMAL),
            col("ADR", "ADR", Type.DECIMAL),
            col("DPR", "DPR", Type.DECIMAL)
    ), "MatchRecordID DESC, PlayerID");

    public static final TableSpec TOURNAMENT_PARTICIPATION = new TableSpec("Tournament Participation", "TournamentParticipation", List.of(
            pkRef("TournamentID", "Tournament ID", Type.INTEGER, "Tournament", "TournamentID"),
            pkRef("TeamName", "Team", Type.TEXT, "Team", "TeamName"),
            col("Placement", "Placement", Type.TEXT)
    ), "TournamentID DESC, TeamName");

    /** Every table, in the order shown in the Manage page selector. */
    public static final List<TableSpec> ALL = List.of(
            PEOPLE,
            PLAYER,
            COACH,
            TEAM,
            PLAYER_CONTRACT,
            COACH_CONTRACT,
            TOURNAMENT,
            MATCH,
            MATCH_RECORD,
            PLAYER_MATCH_STATS,
            TOURNAMENT_PARTICIPATION
    );

    private Schema() {
    }
}
