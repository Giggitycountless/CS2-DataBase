package com.counterstrike.app.repository;

import java.util.List;

/**
 * Curated set of complex analytical queries (joins, aggregation, sub-queries,
 * window-style ranking) exposed on the Analytics page.
 */
public final class AnalyticsCatalog {

    public static final List<AnalyticsQuery> QUERIES = List.of(
            new AnalyticsQuery(
                    "Top players by match performance",
                    "Aggregates per-match stats and ranks players by average in-match rating.",
                    """
                    SELECT p.Nickname AS Nickname,
                           p.FullName AS Full_Name,
                           COUNT(*) AS Maps_Played,
                           ROUND(AVG(pms.Rating), 2) AS Avg_Rating,
                           SUM(pms.Kills) AS Total_Kills,
                           SUM(pms.Deaths) AS Total_Deaths,
                           ROUND(AVG(pms.ADR), 1) AS Avg_ADR
                    FROM PlayerMatchStats pms
                    JOIN People p ON p.PersonID = pms.PlayerID
                    GROUP BY p.Nickname, p.FullName
                    HAVING COUNT(*) >= 1
                    ORDER BY Avg_Rating DESC NULLS LAST, Total_Kills DESC
                    """,
                    null
            ),
            new AnalyticsQuery(
                    "Team win/loss record",
                    "Total matches, wins, losses and win rate per team across all matches.",
                    """
                    SELECT t.TeamName AS Team,
                           t.Region AS Region,
                           COUNT(m.MatchID) AS Matches,
                           SUM(CASE WHEN m.WinnerTeam = t.TeamName THEN 1 ELSE 0 END) AS Wins,
                           SUM(CASE WHEN m.WinnerTeam IS NOT NULL AND m.WinnerTeam <> t.TeamName THEN 1 ELSE 0 END) AS Losses,
                           ROUND(100 * SUM(CASE WHEN m.WinnerTeam = t.TeamName THEN 1 ELSE 0 END)
                                 / NULLIF(COUNT(m.MatchID), 0), 1) AS Win_Pct
                    FROM Team t
                    LEFT JOIN MatchTable m ON m.TeamA = t.TeamName OR m.TeamB = t.TeamName
                    GROUP BY t.TeamName, t.Region
                    ORDER BY Win_Pct DESC NULLS LAST, Wins DESC
                    """,
                    null
            ),
            new AnalyticsQuery(
                    "Active roster of a team",
                    "Current players of a team via active player contracts (no end date or end date in the future).",
                    """
                    SELECT pc.TeamName AS Team,
                           p.Nickname AS Nickname,
                           p.FullName AS Full_Name,
                           pc.InGameRole AS Role,
                           pc.StartDate AS Since
                    FROM PlayerContract pc
                    JOIN People p ON p.PersonID = pc.PersonID
                    WHERE (pc.EndDate IS NULL OR pc.EndDate >= TRUNC(SYSDATE))
                      AND LOWER(pc.TeamName) LIKE ?
                    ORDER BY pc.TeamName, p.Nickname
                    """,
                    "Team name contains"
            ),
            new AnalyticsQuery(
                    "Players above average rating",
                    "Players whose rating exceeds the overall average rating (correlated sub-query).",
                    """
                    SELECT p.Nickname AS Nickname,
                           p.FullName AS Full_Name,
                           pl.Rating AS Rating,
                           pl.ADR AS ADR
                    FROM Player pl
                    JOIN People p ON p.PersonID = pl.PersonID
                    WHERE pl.Rating > (SELECT AVG(Rating) FROM Player)
                    ORDER BY pl.Rating DESC
                    """,
                    null
            ),
            new AnalyticsQuery(
                    "Tournament participation & placements",
                    "Every team's placement per tournament, newest tournaments first.",
                    """
                    SELECT tr.TournamentName AS Tournament,
                           tp.TeamName AS Team,
                           tp.Placement AS Placement,
                           tr.TournyStartDate AS Start_Date
                    FROM TournamentParticipation tp
                    JOIN Tournament tr ON tr.TournamentID = tp.TournamentID
                    ORDER BY tr.TournyStartDate DESC NULLS LAST, tr.TournamentName, tp.Placement
                    """,
                    null
            ),
            new AnalyticsQuery(
                    "Map statistics",
                    "How often each map was played and the most recent time it appeared.",
                    """
                    SELECT mr.Map AS Map,
                           COUNT(*) AS Times_Played,
                           MAX(mr.RecordDate) AS Last_Played
                    FROM MatchRecord mr
                    WHERE mr.Map IS NOT NULL
                    GROUP BY mr.Map
                    ORDER BY Times_Played DESC, Map
                    """,
                    null
            ),
            new AnalyticsQuery(
                    "Coach win-rate ranking",
                    "Coaches ranked by win rate together with their personal details.",
                    """
                    SELECT p.FullName AS Coach,
                           p.Nationality AS Nationality,
                           c.WinRate AS Win_Rate
                    FROM Coach c
                    JOIN People p ON p.PersonID = c.PersonID
                    ORDER BY c.WinRate DESC NULLS LAST
                    """,
                    null
            ),
            new AnalyticsQuery(
                    "Head-to-head matches",
                    "All matches involving a team, with opponent, tournament and winner.",
                    """
                    SELECT m.MatchDate AS Date,
                           m.TeamA AS Team_A,
                           m.TeamB AS Team_B,
                           NVL(m.MatchResultTeamA, '-') || ' : ' || NVL(m.MatchResultTeamB, '-') AS Score,
                           m.WinnerTeam AS Winner,
                           tr.TournamentName AS Tournament
                    FROM MatchTable m
                    JOIN Tournament tr ON tr.TournamentID = m.TournamentID
                    WHERE LOWER(m.TeamA) LIKE ? OR LOWER(m.TeamB) LIKE ?
                    ORDER BY m.MatchDate DESC NULLS LAST, m.MatchID DESC
                    """,
                    "Team name contains"
            )
    );

    private AnalyticsCatalog() {
    }
}
