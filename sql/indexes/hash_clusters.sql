-- =============================================================
-- Milestone 3 – Index Performance Analysis
-- Step 2: HASH CLUSTERS
-- Creates a hash cluster copy of each target table and measures.
-- Run AFTER btree_indexes.sql.
--
-- A hash cluster stores rows with the same key value in the same
-- physical block, giving O(1) point lookups.  For range queries
-- it performs poorly because hash values carry no ordering.
-- =============================================================

SET TIMING   ON
SET ECHO     ON
SET PAGESIZE 9999
SET LINESIZE 200
SPOOL hash_cluster_results.txt

-- ============================================================
-- CLUSTER 1 – for Q1 (point query on PlayerID)
-- ~200,000 rows, 40 distinct PlayerIDs → ~5,000 rows per key.
-- Each row ≈ 50 bytes, so SIZE 8192 allocates ~1 block per key.
-- HASHKEYS 50 provides enough buckets for 40 distinct players.
-- ============================================================

CREATE CLUSTER pms_pid_cluster (player_id NUMBER(10))
    SIZE 8192 HASHKEYS 50;

-- Note: hash clusters do NOT require a separate index (the hash function
-- is the access mechanism).  Only indexed clusters use CREATE INDEX ON CLUSTER.

CREATE TABLE PlayerMatchStats_HC (
    MatchRecordID NUMBER(10)    NOT NULL,
    PlayerID      NUMBER(10)    NOT NULL,
    TeamName      VARCHAR2(100),
    Kills         NUMBER(10)    DEFAULT 0,
    Deaths        NUMBER(10)    DEFAULT 0,
    Assists       NUMBER(10)    DEFAULT 0,
    Rating        NUMBER(3,2),
    ADR           NUMBER(4,1),
    DPR           NUMBER(3,2)
) CLUSTER pms_pid_cluster (PlayerID);

INSERT INTO PlayerMatchStats_HC SELECT * FROM PlayerMatchStats;
COMMIT;

-- ============================================================
-- CLUSTER 2 – for Q2 (range query on RecordDate)
-- ~20,000 rows, many distinct dates.
-- Hash clusters have no ordering so this cluster deliberately
-- performs worse on the range query – demonstrating the
-- limitation of hashing for range access.
-- ============================================================

CREATE CLUSTER mr_date_cluster (record_date DATE)
    SIZE 4096 HASHKEYS 2000;

-- (no CREATE INDEX needed for hash clusters)

CREATE TABLE MatchRecord_HC (
    MatchRecordID   NUMBER(10)   NOT NULL,
    MatchID         NUMBER(10),
    RecordDate      DATE,
    TeamA           VARCHAR2(100),
    TeamB           VARCHAR2(100),
    StartingSide    VARCHAR2(50),
    FinalScore      VARCHAR2(20),
    TopHalfScore    VARCHAR2(20),
    BottomHalfScore VARCHAR2(20),
    TeamAResult     VARCHAR2(50),
    TeamBResult     VARCHAR2(50),
    Map             VARCHAR2(50)
) CLUSTER mr_date_cluster (RecordDate);

INSERT INTO MatchRecord_HC SELECT * FROM MatchRecord;
COMMIT;

EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'PLAYERMATCHSTATS_HC', CASCADE => TRUE);
EXEC DBMS_STATS.GATHER_TABLE_STATS(USER, 'MATCHRECORD_HC',      CASCADE => TRUE);

-- ============================================================
-- Q1 on Hash Cluster (expected: fast – direct block lookup)
-- ============================================================
PROMPT ============================================================
PROMPT  Q1 HASH CLUSTER – Point Query on PlayerMatchStats_HC
PROMPT ============================================================

EXPLAIN PLAN SET STATEMENT_ID = 'Q1_HASH' FOR
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
FROM PlayerMatchStats_HC pms
JOIN People      p  ON p.PersonID         = pms.PlayerID
JOIN MatchRecord mr ON mr.MatchRecordID   = pms.MatchRecordID
JOIN MatchTable  mt ON mt.MatchID         = mr.MatchID
WHERE pms.PlayerID = 11
ORDER BY mt.MatchDate DESC;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'Q1_HASH', 'ALL'));

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
FROM PlayerMatchStats_HC pms
JOIN People      p  ON p.PersonID         = pms.PlayerID
JOIN MatchRecord mr ON mr.MatchRecordID   = pms.MatchRecordID
JOIN MatchTable  mt ON mt.MatchID         = mr.MatchID
WHERE pms.PlayerID = 11
ORDER BY mt.MatchDate DESC;

-- ============================================================
-- Q2 on Hash Cluster (expected: slow – no ordering, full scan)
-- ============================================================
PROMPT ============================================================
PROMPT  Q2 HASH CLUSTER – Range Query on MatchRecord_HC
PROMPT  (expected to be slower than B-Tree for range access)
PROMPT ============================================================

EXPLAIN PLAN SET STATEMENT_ID = 'Q2_HASH' FOR
SELECT
    mr.RecordDate,
    mr.Map,
    mr.TeamA,
    mr.TeamB,
    mr.FinalScore,
    COUNT(pms.PlayerID) AS PlayerCount,
    AVG(pms.Rating)     AS AvgRating,
    MAX(pms.Kills)      AS TopKills
FROM MatchRecord_HC   mr
JOIN PlayerMatchStats pms ON pms.MatchRecordID = mr.MatchRecordID
WHERE mr.RecordDate BETWEEN DATE '2023-01-01' AND DATE '2024-12-31'
GROUP BY mr.RecordDate, mr.Map, mr.TeamA, mr.TeamB, mr.FinalScore
ORDER BY mr.RecordDate;

SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, 'Q2_HASH', 'ALL'));

SELECT
    mr.RecordDate,
    mr.Map,
    mr.TeamA,
    mr.TeamB,
    mr.FinalScore,
    COUNT(pms.PlayerID) AS PlayerCount,
    AVG(pms.Rating)     AS AvgRating,
    MAX(pms.Kills)      AS TopKills
FROM MatchRecord_HC   mr
JOIN PlayerMatchStats pms ON pms.MatchRecordID = mr.MatchRecordID
WHERE mr.RecordDate BETWEEN DATE '2023-01-01' AND DATE '2024-12-31'
GROUP BY mr.RecordDate, mr.Map, mr.TeamA, mr.TeamB, mr.FinalScore
ORDER BY mr.RecordDate;

SPOOL OFF
