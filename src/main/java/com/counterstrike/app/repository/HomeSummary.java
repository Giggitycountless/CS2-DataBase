package com.counterstrike.app.repository;

public record HomeSummary(
        int playerCount,
        int teamCount,
        int matchCount,
        int tournamentCount,
        String topPlayerName,
        String topPlayerDetail,
        String trendingTeamName,
        String trendingTeamDetail
) {
}
