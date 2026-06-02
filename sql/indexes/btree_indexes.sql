-- =============================================================
-- Milestone 3 – Index Performance Analysis
-- Step 1: B-TREE INDEXES
-- Creates one B-Tree index per query, then re-measures.
-- Run AFTER baseline.sql and BEFORE hash_clusters.sql.
-- =============================================================

SET TIMING   ON
SET ECHO     ON
SET PAGESIZE 9999
SET LINESIZE 200
SPOOL btree_results.txt

-- ------------------------------------------------------------
-- Create B-Tree index for Q1: filter on PlayerMatchStats.PlayerID
-- The composite PK (MatchRecordID, PlayerID) exists but cannot
-- efficiently satisfy equality on the second column alone.
-- A dedicated single-column index enables an index range scan.
-- ------------------------------------------------------------
CREATE INDEX idx_pms_playerid
    ON PlayerMatchStats(PlayerID);

-- ------------------------------------------------------------
-- Create B-Tree index for Q2: range filter on MatchRecord.RecordDate
-- ------------------------------------------------------------
CREATE INDEX idx_mr_recorddate
    ON MatchRecord(RecordDate);

-- Refresh optimiser statistics so Oracle picks up the new indexes
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'PLAYERMATCHSTATS', CASCADE => TRUE);
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'MATCHRECORD',      CASCADE => TRUE);

PROMPT ============================================================
PROMPT  Q1 B-TREE – Point Query with idx_pms_playerid
PROMPT ============================================================

EXPLAIN PLAN SET STATEMENT_ID = 'Q1_BTREE' FOR
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

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'Q1_BTREE', 'ALL'));

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
PROMPT  Q2 B-TREE – Range Query with idx_mr_recorddate
PROMPT ============================================================

EXPLAIN PLAN SET STATEMENT_ID = 'Q2_BTREE' FOR
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

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'Q2_BTREE', 'ALL'));

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
