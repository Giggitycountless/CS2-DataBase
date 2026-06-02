# Counter-Strike Database Browser — Defence Notes

> For group members and the live demonstration (Milestones 1–3).
> Covers: tech stack, architecture, database design, MongoDB design,
> index performance analysis, and a full simulated Q&A.

---

## 0. One-Line Summary

A **Java Swing desktop application** backed by **Oracle** and **MongoDB** for managing CS2 esports data: players, coaches, teams, tournaments, matches, match records, and player statistics. The app supports full CRUD across all 11 Oracle tables, 8 preset advanced SQL queries, a MongoDB backend with equivalent read/write, and a Milestone 3 index performance analysis using B-Tree indexes and hash clusters.

---

## 1. Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| UI | Java **Swing** + **FlatLaf 3.4.1** | Modern dark theme (CS2-style) |
| Data access | Raw **JDBC** + **MongoDB Java Driver** | JDBC for Oracle, Driver for MongoDB |
| Oracle | **Oracle Database Free 23** (Docker `gvenzl/oracle-free`) | Service `FREEPDB1` |
| MongoDB | **MongoDB 7.0** (Docker `mongo:7.0`) | Database `cs_small` |
| Build | **Maven** + **maven-shade-plugin** | Fat jar ~8.2 MB, all deps included |
| Runtime | Java 17+ | |

**Why these choices?**
- Raw JDBC instead of an ORM so every SQL statement is visible and controllable — the core point of a database course.
- Docker for both databases: consistent team environment, no local installations.
- MongoDB added for Milestone 3 to demonstrate the contrast between relational and document models.

---

## 2. Architecture (Layered)

```
┌──────────────────────────────────────────────────────────────┐
│  Presentation Layer (UI)                                      │
│  MainFrame · CrudPanel · AnalyticsPanel · Styles             │
└─────────────┬────────────────────────────────────────────────┘
              │ TableData / HomeSummary DTOs
┌─────────────▼────────────────────────────────────────────────┐
│  Repository Layer                                             │
│  AppRepository (interface)                                    │
│  ├── CounterStrikeRepository      ← Oracle implementation    │
│  └── MongoCounterStrikeRepository ← MongoDB implementation   │
│  + Schema / TableSpec / ColumnSpec  (metadata)               │
│  + AnalyticsCatalog               (8 preset queries)         │
└──────┬──────────────────────────┬────────────────────────────┘
       │ JDBC                     │ MongoDB Driver
┌──────▼──────┐          ┌────────▼────────┐
│  Database   │          │  MongoDatabase  │
│  (Oracle)   │          │  (MongoDB)      │
└──────┬──────┘          └────────┬────────┘
       │                          │
┌──────▼──────┐          ┌────────▼────────────┐
│  Oracle DB  │          │  MongoDB cs_small   │
│  11 tables  │          │  4 collections      │
└─────────────┘          └─────────────────────┘
```

The dropdown in `MainFrame` switches between Oracle and MongoDB at runtime. The UI holds an `AppRepository` interface reference — swapping the backend swaps only the implementation, no UI code changes.

---

## 3. Oracle Database Design

### 3.1 The 11 Tables

| Table | Primary Key | Purpose | Key FKs |
|---|---|---|---|
| **People** | PersonID | Common info for all persons | — |
| **Player** | PersonID | Player-specific stats (Rating/ADR/DPR) | PersonID → People |
| **Coach** | PersonID | Coach-specific stats (WinRate) | PersonID → People |
| **Team** | TeamName | Esports team | — |
| **PlayerContract** | PlayerContractID | Player–team contracts | PersonID→Player, TeamName→Team |
| **CoachContract** | CoachContractID | Coach–team contracts | PersonID→Coach, TeamName→Team |
| **Tournament** | TournamentID | Tournament event | — |
| **MatchTable** | MatchID | Match (series level) | TournamentID, TeamA, TeamB, WinnerTeam |
| **MatchRecord** | MatchRecordID | Individual map result | MatchID, TeamA, TeamB |
| **PlayerMatchStats** | (MatchRecordID, PlayerID) | Per-player per-map stats | Three FKs, **composite PK** |
| **TournamentParticipation** | (TournamentID, TeamName) | Participation + placement | Two FKs, **composite PK** |

### 3.2 Key Design Decisions

- **ISA / Generalisation-Specialisation**: `People` is the supertype; `Player` and `Coach` share its primary key (PersonID is both PK and FK to People). This is the standard relational implementation of an ER ISA hierarchy.
- **Composite primary keys**: `PlayerMatchStats(MatchRecordID, PlayerID)` — a player's record is only unique per map. `TournamentParticipation(TournamentID, TeamName)` — a team only participates once per tournament.
- **Referential integrity**: TeamA, TeamB, and WinnerTeam in MatchTable all reference Team, enforced by FKs.
- **Delete strategy**: FKs use default **RESTRICT** (no ON DELETE CASCADE). Attempting to delete a parent row with children raises ORA-02292, which the app translates into a readable message. This prevents accidental cascade deletes.
- **Third Normal Form (3NF)**: Every non-key attribute depends directly on the primary key. Player/coach-specific attributes are in separate tables to avoid nulls in People.

### 3.3 Dataset Scale

| Table | Small dataset | Large dataset |
|---|---|---|
| People / Player / Coach / Team | 48 / 40 / 8 / 8 | same |
| Tournament | 1 | 200 |
| MatchTable | 7 | 20,200 |
| MatchRecord | 18 | 20,000 |
| PlayerMatchStats | ~60 | **200,000** |

---

## 4. MongoDB Design

### 4.1 Database and Collections

Database: `cs_small` — 4 collections:

| Collection | Docs | Structure |
|---|---|---|
| `teams` | 8 | `{ _id: "Team Spirit", region: "CIS" }` |
| `people` | 10 | Person info + embedded playerStats/coachStats + embedded contract |
| `tournaments` | 1 | Tournament info + embedded participation array |
| `matches` | 7 | Match info + embedded records array, each record embeds playerStats |

### 4.2 Example Document (people collection)

```json
{
  "_id": 11,
  "nickname": "donk",
  "fullName": "Danil Kryshkovets",
  "birthday": ISODate("2007-01-25"),
  "nationality": "Russia",
  "type": "player",
  "playerStats": { "rating": 1.32, "adr": 90.4, "dpr": 0.66 },
  "contract": {
    "teamName": "Team Spirit",
    "startDate": ISODate("2023-07-01"),
    "endDate": null,
    "inGameRole": "Rifler"
  }
}
```

### 4.3 MongoDB vs Oracle Comparison

| Dimension | Oracle (Relational) | MongoDB (Document) |
|---|---|---|
| Structure | 11 normalised tables | 4 collections with embedded data |
| Player + contract | People + Player/Coach + Contract (4 tables, JOIN) | One people document, stats and contract embedded |
| Match + stats | MatchTable + MatchRecord + PlayerMatchStats (3 tables) | One matches document, records and playerStats embedded |
| Read one player's full info | Multi-table JOIN required | Single document read, O(1) |
| Range queries / aggregation | Powerful SQL, any GROUP BY | Aggregation pipeline, more verbose |
| Data integrity | FK constraints enforced by DB | Application-level responsibility |
| Best suited for | Fixed schema, complex relationships, strong consistency | Hierarchical data, read-heavy, flexible schema |

**Why embed rather than reference?** The most common read pattern is "all info for one player" and "all stats for one match." Embedding related data in the same document avoids joins and makes these reads very fast.

---

## 5. Milestone 3 — Index Performance Analysis

### 5.1 The Two High-Load Queries

**Q1 — Point Query**: All match stats for a specific player (PlayerID = 11, "donk").
Filters on PlayerMatchStats (200,000 rows) with an equality predicate, joins 4 tables.

**Q2 — Range Query**: Aggregate match stats for maps played within a date range.
Filters on MatchRecord (20,000 rows) with BETWEEN on RecordDate, joins PlayerMatchStats for aggregation.

### 5.2 Performance Results

**Q1 — Point Query**

| Scenario | Key EXPLAIN PLAN step | Elapsed |
|---|---|---|
| Baseline (no extra index) | TABLE ACCESS FULL on PLAYERMATCHSTATS | **0.34s** |
| B-Tree index (`idx_pms_playerid`) | TABLE ACCESS FULL (optimizer rejected the index) | **0.17s** |
| Hash cluster (`pms_pid_cluster`) | TABLE ACCESS HASH on PLAYERMATCHSTATS_HC | **2.92s** |

**Q2 — Range Query**

| Scenario | Key EXPLAIN PLAN step | Elapsed |
|---|---|---|
| Baseline (no extra index) | TABLE ACCESS FULL on MATCHRECORD | **0.02s** |
| B-Tree index (`idx_mr_recorddate`) | INDEX RANGE SCAN on IDX_MR_RECORDDATE | **0.02s** |
| Hash cluster (`mr_date_cluster`) | TABLE ACCESS FULL on MATCHRECORD_HC (cost=555 vs 68) | **0.02s** |

### 5.3 Analysis

**Q1 B-Tree index not used**: Oracle's cost-based optimiser estimated ~5,046 rows returned (~2.5% of the table). A full table scan using multi-block sequential I/O (cost 342) was cheaper than 5,046 random block reads via the index. B-Tree indexes are most effective at **high selectivity (<1%)**.

**Q1 hash cluster slower (2.92s)**: The `SIZE=8192` parameter was far too small for the actual data per cluster key (~250KB), causing extensive block chaining. The cluster had to follow overflow chains to retrieve all rows for PlayerID=11, resulting in more I/O than a plain full scan. Correct sizing requires careful estimation.

**Q2 B-Tree index used successfully**: EXPLAIN PLAN confirms the switch from TABLE ACCESS FULL to INDEX RANGE SCAN (cost drops from 68 to 4). **B-Tree indexes store keys in sorted order, making range scans efficient** — only the relevant leaf blocks need to be read.

**Q2 hash cluster cannot range-scan**: The hash function maps keys to random buckets with no ordering. A BETWEEN predicate requires examining all buckets (full scan), raising cost from 68 to 555. This confirms that **hash structures are unsuitable for range access**.

---

## 6. Core Implementation Principles

### 6.1 Metadata-Driven CRUD (the key design)

Instead of writing separate insert/update/delete logic for all 11 tables, three metadata classes describe the schema:
- `ColumnSpec`: column name, type (TEXT/INTEGER/DECIMAL/DATE), PK/FK/required flags
- `TableSpec`: table name, column list, primary key columns
- `Schema`: central definition of all 11 tables

A single generic code path reads this metadata and generates the SQL at runtime:
```java
String sql = "INSERT INTO " + spec.tableName() + " (" + names + ") VALUES (" + marks + ")";
```
Values are still bound via `PreparedStatement` placeholders — never string-concatenated.

The UI is also metadata-driven: `CrudPanel` reads `TableSpec` and generates the form automatically — FK columns become dropdowns (options fetched from the referenced table), date columns show format hints.

**Value**: adding a new table or column requires only one change to `Schema.java`. The UI, browse, insert, update, and delete all update automatically.

### 6.2 SQL Injection Prevention

All user input is bound via `PreparedStatement` `?` placeholders — the database treats the value as data, never as SQL code. Table and column names come only from `Schema` metadata (a whitelist in our own code), never from user input.

### 6.3 Transactions and ACID

```java
connection.setAutoCommit(false);
try {
    statement.executeUpdate();
    connection.commit();       // success: persist changes
} catch (SQLException e) {
    connection.rollback();     // failure: undo all changes
}
```
Guarantees atomicity: every write either completes fully or leaves the database unchanged.

### 6.4 SwingWorker and Thread Safety

All database queries run in `SwingWorker.doInBackground()` (a background thread). Results are handed back to the Event Dispatch Thread (EDT) in `done()` to update the UI. This keeps the interface responsive during slow queries.

### 6.5 The 8 Preset Advanced Queries

| Report | SQL Techniques |
|---|---|
| Player tournament performance | Multi-table JOIN, GROUP BY, AVG/SUM/COUNT, HAVING |
| Team win/loss record | LEFT JOIN, CASE WHEN conditional counts, NULLIF to prevent divide-by-zero |
| Current team roster | JOIN with date condition (active contracts) |
| Above-average rated players | Subquery: WHERE Rating > (SELECT AVG…) |
| Tournament standings | Multi-table JOIN, ORDER BY |
| Map statistics | GROUP BY, COUNT, MAX |
| Coach win-rate ranking | JOIN, ORDER BY … NULLS LAST |
| Head-to-head record | JOIN, parameterised LIKE |

---

## 7. Quick Start

```powershell
.\scripts\start-oracle.ps1           # start Oracle
cd mongodb; docker compose up -d; cd ..  # start MongoDB
java -jar target\counterstrike-browser-1.0.0.jar
```

For Milestone 3 index analysis (one-off):
```powershell
.\scripts\run-index-analysis.ps1
```

---

## 8. Known Improvement Points

1. **Connection pooling**: Every operation opens a new connection. Production code should use HikariCP.
2. **Pagination**: `browse` fetches the whole table. For very large datasets, use `OFFSET … FETCH`.
3. **Hash cluster sizing**: `SIZE` must be estimated accurately from actual row count × row size, otherwise overflow chaining negates the benefit.
4. **Optimistic locking**: No version column prevents concurrent write conflicts.
5. **MongoDB transactions**: Writes across multiple collections are not atomic in the current implementation.

---

# Simulated Q&A

## A. Architecture

**Q: Why use so many layers? Why not just write SQL in the button handler?**
A: It works, but becomes unmaintainable. With layers, the UI doesn't know about SQL, SQL doesn't know about the UI, and the database connection is isolated. Changing the UI doesn't touch any SQL. Swapping Oracle for another database only requires changing the connection layer. This is separation of concerns — lower coupling, easier to maintain and test.

**Q: What does the AppRepository interface achieve?**
A: Both Oracle and MongoDB implementations implement the same interface. The UI holds an interface reference, so swapping the backend at runtime only swaps the implementing class — zero UI code changes. This is the dependency inversion principle.

**Q: What is a DTO?**
A: A simple data container passed between layers. `TableData` holds column names and rows. The UI never touches a `ResultSet` directly — it just reads a `TableData`. This decouples the UI from JDBC.

## B. Database Theory

**Q: What normal form do your tables satisfy?**
A: Third Normal Form (3NF). Every non-key attribute depends directly on the whole primary key — no partial dependencies and no transitive dependencies. For example, player-specific stats are in the Player table, not People, to avoid nulls and redundancy.

**Q: What is the relationship between People, Player, and Coach?**
A: ISA (generalisation-specialisation, or inheritance). People stores common attributes. Player and Coach store specialist attributes and share the primary key PersonID, which is also a foreign key to People. Whether a person is a player or coach depends on which sub-table they have a row in.

**Q: Why composite primary keys?**
A: `PlayerMatchStats(MatchRecordID, PlayerID)` — a player's stats are only unique per map. `TournamentParticipation(TournamentID, TeamName)` — a team only appears once per tournament. Neither column alone identifies a row uniquely.

**Q: What is ACID? How does your application demonstrate it?**
A: Atomicity, Consistency, Isolation, Durability. My write operations use `setAutoCommit(false)` + `commit/rollback`: if anything fails, the entire operation is rolled back, leaving the database unchanged. This demonstrates atomicity and consistency. Isolation and durability are provided by Oracle.

**Q: Why not use ON DELETE CASCADE on foreign keys?**
A: Cascade delete removes a parent row and silently deletes all child rows. During a demo or if a user makes a mistake, this could wipe a large amount of related data. Using RESTRICT forces the user to delete child records first. The app translates ORA-02292 into a readable message explaining what to do. This is safer and more controlled.

## C. SQL

**Q: What is the difference between INNER JOIN and LEFT JOIN? Where do you use LEFT JOIN?**
A: INNER JOIN returns only rows that match in both tables. LEFT JOIN returns all rows from the left table, with NULLs for unmatched right-side columns. The "team win/loss record" query uses LEFT JOIN so that teams with no matches at all still appear in the results with a count of zero.

**Q: What is the difference between WHERE and HAVING?**
A: WHERE filters rows before grouping. HAVING filters groups after aggregation. For example, `HAVING COUNT(*) >= 1` filters groups by aggregate result — it cannot be in WHERE because the aggregate doesn't exist yet at the WHERE stage.

**Q: Where do you use a subquery?**
A: "Above-average rated players": `WHERE Rating > (SELECT AVG(Rating) FROM Player)`. The subquery calculates the overall average first; the outer query then filters players above it.

**Q: How do you prevent SQL injection?**
A: All user input is bound via `PreparedStatement` `?` placeholders. The database engine treats the value as data, not as SQL code. Table and column names are never accepted from user input — they come only from `Schema` metadata defined in our code (a hard-coded whitelist).

## D. Milestone 3 — Indexing and MongoDB

**Q: Why didn't the B-Tree index improve Q1?**
A: Oracle's cost-based optimiser estimated that PlayerID=11 returns about 5,046 rows — roughly 2.5% of the table. For that selectivity, a full table scan using multi-block sequential I/O (cost 342) is cheaper than 5,046 individual random block reads through the index. B-Tree indexes give the greatest benefit when selectivity is high (typically below 1%).

**Q: Why was the hash cluster slower for Q1?**
A: The `SIZE=8192` parameter allocated only about 8 KB per hash bucket, but each PlayerID has around 250 KB of data (5,000 rows × 50 bytes). Oracle had to follow many overflow chain blocks to collect all rows for one player, resulting in more I/O than a plain full scan. To get the O(1) benefit of a hash cluster, SIZE must accurately reflect the data volume per key.

**Q: Why can't a hash cluster do a range query?**
A: A hash function maps keys to buckets with no ordering. There is no way to identify which buckets contain dates in a range without checking all of them — a full scan. A B-Tree index stores keys in sorted order, so a range scan reads only the relevant leaf blocks sequentially. The cost of the hash cluster for Q2 was 555 versus 4 for the B-Tree index, demonstrating this clearly.

**Q: What is the biggest design difference between MongoDB and Oracle in your project?**
A: Oracle uses 11 normalised tables connected by foreign keys. Reading one player's complete information requires joining People, Player, and PlayerContract. MongoDB uses four collections where related data is embedded in the same document. Reading one player's full information is a single document fetch. The trade-off is that MongoDB loses database-enforced referential integrity; the application must ensure consistency itself.

**Q: When would you choose MongoDB over Oracle?**
A: MongoDB suits hierarchical, read-heavy data with a flexible or evolving schema and no need for cross-document transactions. Oracle suits complex relational data with strict consistency requirements, foreign key constraints, and advanced SQL analytics. For this domain — players, contracts, tournaments with complex relationships — Oracle is the more natural fit. MongoDB demonstrates an alternative data model and is useful when the access pattern favours reading whole nested objects.

**Q: Why does your MongoDB database have only 4 collections when Oracle has 11 tables?**
A: MongoDB uses document embedding to group related data together. What Oracle represents as three tables (People + Player + PlayerContract) becomes a single `people` document with embedded stats and contract. This reduces the collection count but means there is no database-level constraint to enforce the integrity of the embedded data — that responsibility moves to the application code.
