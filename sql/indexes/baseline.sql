-- =============================================================
-- Milestone 3 – Index Performance Analysis
-- Step 0: BASELINE (no extra indexes)
-- Run BEFORE creating any indexes.
-- Usage: sqlplus CS2/cs2_password@localhost:1521/FREEPDB1 @baseline.sql
-- =============================================================

SET TIMING   ON
SET ECHO     ON
SET PAGESIZE 9999
SET LINESIZE 200
SPOOL baseline_results.txt

PROMPT ============================================================
PROMPT  Q1 BASELINE – Point Query: all stats for PlayerID = 11
PROMPT ============================================================

EXPLAIN PLAN SET STATEMENT_ID = 'Q1_BASE' FOR
SELECT
    p.Nickname,
    pms.Kills,
    pms.Deaths,
    pms.Assists,
    pms.Rating,
    pms.ADR,
    mr.Map,
    mt.Stage,
    mt.MatchDate
FROM PlayerMatchStats pms
JOIN People      p  ON p.PersonID         = pms.PlayerID
JOIN MatchRecord mr ON mr.MatchRecordID   = pms.MatchRecordID
JOIN MatchTable  mt ON mt.MatchID         = mr.MatchID
WHERE pms.PlayerID = 11
ORDER BY mt.MatchDate DESC;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'Q1_BASE', 'ALL'));

-- Timed execution
SELECT
    p.Nickname,
    pms.Kills,
    pms.Deaths,
    pms.Assists,
    pms.Rating,
    pms.ADR,
    mr.Map,
    mt.Stage,
    mt.MatchDate
FROM PlayerMatchStats pms
JOIN People      p  ON p.PersonID         = pms.PlayerID
JOIN MatchRecord mr ON mr.MatchRecordID   = pms.MatchRecordID
JOIN MatchTable  mt ON mt.MatchID         = mr.MatchID
WHERE pms.PlayerID = 11
ORDER BY mt.MatchDate DESC;

PROMPT ============================================================
PROMPT  Q2 BASELINE – Range Query: match records 2023-01-01 to
PROMPT                             2024-12-31 with aggregate stats
PROMPT ============================================================

EXPLAIN PLAN SET STATEMENT_ID = 'Q2_BASE' FOR
SELECT
    mr.RecordDate,
    mr.Map,
    mr.TeamA,
    mr.TeamB,
    mr.FinalScore,
    COUNT(pms.PlayerID) AS PlayerCount,
    AVG(pms.Rating)     AS AvgRating,
    MAX(pms.Kills)      AS TopKills
FROM MatchRecord     mr
JOIN PlayerMatchStats pms ON pms.MatchRecordID = mr.MatchRecordID
WHERE mr.RecordDate BETWEEN DATE '2023-01-01' AND DATE '2024-12-31'
GROUP BY mr.RecordDate, mr.Map, mr.TeamA, mr.TeamB, mr.FinalScore
ORDER BY mr.RecordDate;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'Q2_BASE', 'ALL'));

-- Timed execution
SELECT
    mr.RecordDate,
    mr.Map,
    mr.TeamA,
    mr.TeamB,
    mr.FinalScore,
    COUNT(pms.PlayerID) AS PlayerCount,
    AVG(pms.Rating)     AS AvgRating,
    MAX(pms.Kills)      AS TopKills
FROM MatchRecord     mr
JOIN PlayerMatchStats pms ON pms.MatchRecordID = mr.MatchRecordID
WHERE mr.RecordDate BETWEEN DATE '2023-01-01' AND DATE '2024-12-31'
GROUP BY mr.RecordDate, mr.Map, mr.TeamA, mr.TeamB, mr.FinalScore
ORDER BY mr.RecordDate;

SPOOL OFF
