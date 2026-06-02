-- =============================================================
-- Milestone 3 – Cleanup
-- Drops all indexes and hash cluster objects created for the
-- performance analysis.  Safe to re-run (errors ignored).
-- =============================================================

SET ECHO ON

-- Drop cluster tables (must drop before the cluster itself)
BEGIN EXECUTE IMMEDIATE 'DROP TABLE PlayerMatchStats_HC'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE MatchRecord_HC';      EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- Drop clusters
BEGIN EXECUTE IMMEDIATE 'DROP CLUSTER pms_pid_cluster';  EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP CLUSTER mr_date_cluster';  EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- Drop B-Tree indexes
BEGIN EXECUTE IMMEDIATE 'DROP INDEX idx_pms_playerid';   EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP INDEX idx_mr_recorddate';  EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- Clear PLAN_TABLE for this session
DELETE FROM PLAN_TABLE WHERE STATEMENT_ID IN ('Q1_BASE','Q2_BASE','Q1_BTREE','Q2_BTREE','Q1_HASH','Q2_HASH');
COMMIT;

PROMPT Cleanup complete.
