package com.counterstrike.app.repository;

import java.util.List;
import java.util.Map;

public interface AppRepository {
    TableData findTopPlayers(String search) throws Exception;
    TableData findTrendingTeams(String search) throws Exception;
    TableData findTeamMatchHistory(String search) throws Exception;
    TableData findTournamentMatchSummary(String search) throws Exception;
    TableData findPeople(String search) throws Exception;
    TableData findTeamsBasic(String search) throws Exception;
    TableData findTournamentsBasic(String search) throws Exception;
    void addTeam(String teamName, String region) throws Exception;
    void updateTeamRegion(String teamName, String region) throws Exception;
    HomeSummary loadHomeSummary() throws Exception;
    TableData browse(TableSpec spec, String search) throws Exception;
    List<String> distinctValues(String table, String column) throws Exception;
    void insertRow(TableSpec spec, Map<String, String> rawValues) throws Exception;
    void updateRow(TableSpec spec, Map<String, String> primaryKey, Map<String, String> rawValues) throws Exception;
    void deleteRow(TableSpec spec, Map<String, String> primaryKey) throws Exception;
    TableData runAnalytics(AnalyticsQuery query, String parameter) throws Exception;
    boolean supportsAnalytics();
    String dbLabel();
}
