INSERT INTO People (PersonID, FullName, Birthday, Nationality) VALUES (101, 'Player 1', DATE '2001-05-14', 'New Zealand');
INSERT INTO People (PersonID, FullName, Birthday, Nationality) VALUES (102, 'Player 2', DATE '2000-08-02', 'Australia');
INSERT INTO People (PersonID, FullName, Birthday, Nationality) VALUES (103, 'Player 3', DATE '1999-11-20', 'New Zealand');
INSERT INTO People (PersonID, FullName, Birthday, Nationality) VALUES (104, 'Player 4', DATE '2002-03-10', 'New Zealand');
INSERT INTO People (PersonID, FullName, Birthday, Nationality) VALUES (105, 'Player 5', DATE '1998-12-05', 'Australia');
INSERT INTO People (PersonID, FullName, Birthday, Nationality) VALUES (106, 'Player 6', DATE '2003-07-19', 'New Zealand');
INSERT INTO People (PersonID, FullName, Birthday, Nationality) VALUES (201, 'Coach 1', DATE '1985-01-11', 'New Zealand');
INSERT INTO People (PersonID, FullName, Birthday, Nationality) VALUES (202, 'Coach 2', DATE '1988-04-23', 'Australia');

INSERT INTO Player (PersonID, Rating, ADR, DPR) VALUES (101, 1.35, 95.00, 0.60);
INSERT INTO Player (PersonID, Rating, ADR, DPR) VALUES (102, 1.30, 92.00, 0.62);
INSERT INTO Player (PersonID, Rating, ADR, DPR) VALUES (103, 1.25, 90.00, 0.65);
INSERT INTO Player (PersonID, Rating, ADR, DPR) VALUES (104, 1.18, 86.50, 0.68);
INSERT INTO Player (PersonID, Rating, ADR, DPR) VALUES (105, 1.12, 82.75, 0.70);
INSERT INTO Player (PersonID, Rating, ADR, DPR) VALUES (106, 1.05, 79.25, 0.74);

INSERT INTO Coach (PersonID, WinRate) VALUES (201, 67.50);
INSERT INTO Coach (PersonID, WinRate) VALUES (202, 62.20);

INSERT INTO Team (TeamName, Region) VALUES ('Team 1', 'Waikato');
INSERT INTO Team (TeamName, Region) VALUES ('Team 2', 'Bay of Plenty');
INSERT INTO Team (TeamName, Region) VALUES ('Team 3', 'Waikato');
INSERT INTO Team (TeamName, Region) VALUES ('Team 4', 'Auckland');
INSERT INTO Team (TeamName, Region) VALUES ('Team 5', 'Wellington');

INSERT INTO PlayerContract (PlayerContractID, PersonID, TeamName, StartDate, EndDate, InGameRole) VALUES (1, 101, 'Team 1', DATE '2025-01-01', NULL, 'Rifler');
INSERT INTO PlayerContract (PlayerContractID, PersonID, TeamName, StartDate, EndDate, InGameRole) VALUES (2, 102, 'Team 1', DATE '2025-01-01', NULL, 'AWPer');
INSERT INTO PlayerContract (PlayerContractID, PersonID, TeamName, StartDate, EndDate, InGameRole) VALUES (3, 103, 'Team 2', DATE '2025-01-15', NULL, 'Entry');
INSERT INTO PlayerContract (PlayerContractID, PersonID, TeamName, StartDate, EndDate, InGameRole) VALUES (4, 104, 'Team 3', DATE '2025-02-01', NULL, 'Support');
INSERT INTO PlayerContract (PlayerContractID, PersonID, TeamName, StartDate, EndDate, InGameRole) VALUES (5, 105, 'Team 4', DATE '2025-02-01', NULL, 'IGL');
INSERT INTO PlayerContract (PlayerContractID, PersonID, TeamName, StartDate, EndDate, InGameRole) VALUES (6, 106, 'Team 5', DATE '2025-02-10', NULL, 'Lurker');

INSERT INTO CoachContract (CoachContractID, PersonID, TeamName, StartDate, EndDate) VALUES (1, 201, 'Team 1', DATE '2025-01-01', NULL);
INSERT INTO CoachContract (CoachContractID, PersonID, TeamName, StartDate, EndDate) VALUES (2, 202, 'Team 2', DATE '2025-01-01', NULL);

INSERT INTO Tournament (TournamentID, TournamentName, TournyStartDate, TournyEndDate) VALUES (1, 'Tournament 1 2026', DATE '2026-02-10', DATE '2026-02-20');
INSERT INTO Tournament (TournamentID, TournamentName, TournyStartDate, TournyEndDate) VALUES (2, 'Tournament 2 2026', DATE '2026-01-15', DATE '2026-01-25');
INSERT INTO Tournament (TournamentID, TournamentName, TournyStartDate, TournyEndDate) VALUES (3, 'Tournament 3 2025', DATE '2025-12-01', DATE '2025-12-10');
INSERT INTO Tournament (TournamentID, TournamentName, TournyStartDate, TournyEndDate) VALUES (4, 'Tournament 4 2025', DATE '2025-11-05', DATE '2025-11-12');

INSERT INTO MatchTable (MatchID, TournamentID, MatchDate, Stage, TeamA, TeamB, MatchResultTeamA, MatchResultTeamB, WinnerTeam, WinningCondition)
VALUES (1, 1, DATE '2026-02-20', 'Final', 'Team 1', 'Team 2', '2', '1', 'Team 1', 'Best of 3');
INSERT INTO MatchTable (MatchID, TournamentID, MatchDate, Stage, TeamA, TeamB, MatchResultTeamA, MatchResultTeamB, WinnerTeam, WinningCondition)
VALUES (2, 2, DATE '2026-02-19', 'Semi Final', 'Team 2', 'Team 3', '2', '0', 'Team 2', 'Best of 3');
INSERT INTO MatchTable (MatchID, TournamentID, MatchDate, Stage, TeamA, TeamB, MatchResultTeamA, MatchResultTeamB, WinnerTeam, WinningCondition)
VALUES (3, 3, DATE '2026-01-25', 'Group', 'Team 3', 'Team 4', '2', '0', 'Team 3', 'Best of 3');
INSERT INTO MatchTable (MatchID, TournamentID, MatchDate, Stage, TeamA, TeamB, MatchResultTeamA, MatchResultTeamB, WinnerTeam, WinningCondition)
VALUES (4, 4, DATE '2026-01-22', 'Group', 'Team 4', 'Team 5', '2', '1', 'Team 4', 'Best of 3');
INSERT INTO MatchTable (MatchID, TournamentID, MatchDate, Stage, TeamA, TeamB, MatchResultTeamA, MatchResultTeamB, WinnerTeam, WinningCondition)
VALUES (5, 1, DATE '2026-02-18', 'Semi Final', 'Team 1', 'Team 3', '2', '0', 'Team 1', 'Best of 3');
INSERT INTO MatchTable (MatchID, TournamentID, MatchDate, Stage, TeamA, TeamB, MatchResultTeamA, MatchResultTeamB, WinnerTeam, WinningCondition)
VALUES (6, 2, DATE '2026-01-20', 'Group', 'Team 5', 'Team 1', '1', '2', 'Team 1', 'Best of 3');

INSERT INTO MatchRecord (MatchRecordID, MatchID, RecordDate, TeamA, TeamB, StartingSide, FinalScore, TopHalfScore, BottomHalfScore, TeamAResult, TeamBResult, Map)
VALUES (1, 1, DATE '2026-02-20', 'Team 1', 'Team 2', 'CT', '13-11', '7-5', '6-6', 'Win', 'Loss', 'Mirage');
INSERT INTO MatchRecord (MatchRecordID, MatchID, RecordDate, TeamA, TeamB, StartingSide, FinalScore, TopHalfScore, BottomHalfScore, TeamAResult, TeamBResult, Map)
VALUES (2, 2, DATE '2026-02-19', 'Team 2', 'Team 3', 'T', '13-7', '8-4', '5-3', 'Win', 'Loss', 'Inferno');
INSERT INTO MatchRecord (MatchRecordID, MatchID, RecordDate, TeamA, TeamB, StartingSide, FinalScore, TopHalfScore, BottomHalfScore, TeamAResult, TeamBResult, Map)
VALUES (3, 3, DATE '2026-01-25', 'Team 3', 'Team 4', 'CT', '13-6', '9-3', '4-3', 'Win', 'Loss', 'Nuke');
INSERT INTO MatchRecord (MatchRecordID, MatchID, RecordDate, TeamA, TeamB, StartingSide, FinalScore, TopHalfScore, BottomHalfScore, TeamAResult, TeamBResult, Map)
VALUES (4, 4, DATE '2026-01-22', 'Team 4', 'Team 5', 'T', '13-10', '6-6', '7-4', 'Win', 'Loss', 'Ancient');

INSERT INTO PlayerMatchStats (MatchRecordID, PlayerID, TeamName, Kills, Deaths, Assists, Rating, ADR, DPR)
VALUES (1, 101, 'Team 1', 24, 13, 5, 1.35, 95.0, 0.60);
INSERT INTO PlayerMatchStats (MatchRecordID, PlayerID, TeamName, Kills, Deaths, Assists, Rating, ADR, DPR)
VALUES (1, 103, 'Team 2', 18, 16, 4, 1.25, 90.0, 0.65);
INSERT INTO PlayerMatchStats (MatchRecordID, PlayerID, TeamName, Kills, Deaths, Assists, Rating, ADR, DPR)
VALUES (2, 103, 'Team 2', 21, 12, 7, 1.25, 90.0, 0.65);
INSERT INTO PlayerMatchStats (MatchRecordID, PlayerID, TeamName, Kills, Deaths, Assists, Rating, ADR, DPR)
VALUES (3, 104, 'Team 3', 19, 14, 6, 1.18, 86.5, 0.68);

INSERT INTO TournamentParticipation (TournamentID, TeamName, Placement) VALUES (1, 'Team 1', '1st');
INSERT INTO TournamentParticipation (TournamentID, TeamName, Placement) VALUES (1, 'Team 2', '2nd');
INSERT INTO TournamentParticipation (TournamentID, TeamName, Placement) VALUES (1, 'Team 3', '3rd');
INSERT INTO TournamentParticipation (TournamentID, TeamName, Placement) VALUES (2, 'Team 1', '1st');
INSERT INTO TournamentParticipation (TournamentID, TeamName, Placement) VALUES (2, 'Team 2', '2nd');
INSERT INTO TournamentParticipation (TournamentID, TeamName, Placement) VALUES (3, 'Team 3', '1st');
INSERT INTO TournamentParticipation (TournamentID, TeamName, Placement) VALUES (4, 'Team 4', '1st');

COMMIT;
