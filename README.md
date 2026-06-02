# Counter-Strike 2 Database Browser

A Java Swing desktop application backed by Oracle and MongoDB, built for the COMPX323 Advanced Database Concepts group project. The domain is Counter-Strike 2 esports: players, coaches, teams, tournaments, matches, match records, and player match statistics.

## Milestones

- **Milestone 1** – ER diagram and conceptual design
- **Milestone 2** – Oracle relational database, datasets, and Java application with full CRUD and advanced SQL queries
- **Milestone 3** – MongoDB extension, B-Tree index and hash cluster performance analysis

## Features

- Home dashboard with row counts and trending team panel
- **Manage** page: browse all 11 tables, search, column filters (`>`, `<`, `=`, `contains`, …), and full Add / Edit / Delete for every table
- **Analytics** page: 8 preset advanced queries (multi-table joins, aggregation, subqueries, parameterised search)
- Oracle ↔ MongoDB selector: switch the backend at runtime
- MongoDB read, display, and write support with error handling
- Error-proof: all user input validated, FK/constraint violations shown as readable messages

## Technology Stack

| Layer | Technology |
|---|---|
| UI | Java Swing + FlatLaf 3.4.1 (dark theme) |
| Data access | JDBC (Oracle) + MongoDB Java Driver |
| Oracle | Oracle Database Free 23 in Docker (`gvenzl/oracle-free`) |
| MongoDB | MongoDB 7.0 in Docker (`mongo:7.0`) |
| Build | Maven + maven-shade-plugin (fat jar) |
| Runtime | Java 17+ |

## Project Structure

```
CS2-DataBase/
│
├── app.properties                        # Oracle connection settings (local, not committed)
├── pom.xml                               # Maven build file and dependencies
│
├── src/main/java/com/counterstrike/app/
│   ├── CounterStrikeApp.java             # Entry point – initialises theme, DB, and main window
│   ├── config/
│   │   └── DatabaseConfig.java          # Reads app.properties, holds connection parameters
│   ├── db/
│   │   ├── Database.java                # Oracle JDBC connection factory
│   │   └── MongoDatabase.java           # MongoDB connection factory
│   ├── repository/
│   │   ├── AppRepository.java           # Shared interface (browse / insert / update / delete)
│   │   ├── CounterStrikeRepository.java # Oracle implementation – all SQL, CRUD, error mapping
│   │   ├── MongoCounterStrikeRepository.java  # MongoDB implementation – document read/write
│   │   ├── Schema.java                  # Metadata for all 11 tables (types, PKs, FKs)
│   │   ├── TableSpec.java               # Per-table metadata (name, columns, primary key)
│   │   ├── ColumnSpec.java              # Per-column metadata (type, PK/FK/required flags)
│   │   ├── TableData.java               # Query result container (column names + rows)
│   │   ├── HomeSummary.java             # DTO for home-page counts and trending data
│   │   ├── AnalyticsQuery.java          # Single advanced query definition (name, SQL, param)
│   │   └── AnalyticsCatalog.java        # Catalogue of 8 preset advanced queries
│   └── ui/
│       ├── MainFrame.java               # Main window – nav bar and page switching
│       ├── CrudPanel.java               # Manage page – table selector, search, filters, forms
│       ├── AnalyticsPanel.java          # Analytics page – query picker, param input, results
│       ├── ReadOnlyTableModel.java      # Swing table data model
│       └── Styles.java                  # Shared dark-theme colours, fonts, button/table styles
│
├── src/main/resources/
│   └── app.properties.example           # Connection config template for teammates
│
├── sql/
│   ├── schema.sql                        # Oracle DDL – all 11 tables, PKs, FKs, constraints
│   ├── seed.sql                          # Oracle small demo dataset
│   └── indexes/                          # Milestone 3 – index performance analysis
│       ├── baseline.sql                  # Measure both queries with no extra indexes + EXPLAIN PLAN
│       ├── btree_indexes.sql             # Create B-Tree indexes, re-measure
│       ├── hash_clusters.sql             # Create hash clusters, re-measure
│       ├── cleanup.sql                   # Drop all created indexes and cluster objects
│       ├── baseline_results.txt          # Actual baseline timing and execution plans
│       ├── btree_results.txt             # B-Tree timing and execution plans
│       └── hash_cluster_results.txt      # Hash cluster timing and execution plans
│
├── mongodb/
│   ├── docker-compose.yml               # Starts MongoDB container (cs_mongo_small, port 27017)
│   └── scripts/
│       ├── init.js                      # Initialises cs_small DB (collections, data, indexes)
│       └── demo_queries.js              # Example MongoDB queries for reference
│
├── scripts/
│   ├── start-oracle.ps1                 # Start Oracle Docker container
│   ├── import-main-data.ps1             # Recreate schema and import large dataset into Oracle
│   ├── run-index-analysis.ps1           # Run full Milestone 3 index analysis in three steps
│   ├── build.ps1                        # Fallback javac build (use mvn instead)
│   └── run.ps1                          # Fallback run script
│
├── Milestone2Files/
│   ├── mega_dataset.sql                 # Large dataset (~200,000 rows for PlayerMatchStats etc.)
│   ├── smallDataSet.txt                 # Small dataset (5–10 rows per primary table)
│   ├── Project_Milestone2_Counterstrike_all table sql query.txt
│   └── testCodes.txt
│
└── docs/
    └── 系统原理与答辩文档.md             # Architecture explanation and defence Q&A (Chinese)
```

## Quick Start

### Prerequisites

- Java 17+, Maven 3.9+, Docker Desktop

### Oracle Setup (first time only)

```powershell
docker run -d --name cs2-oracle -p 1521:1521 `
  -e ORACLE_PASSWORD=oracle_admin_123 `
  -e APP_USER=CS2 `
  -e APP_USER_PASSWORD=cs2_password `
  gvenzl/oracle-free:23-slim-faststart
```

Wait for `DATABASE IS READY TO USE!` in the logs, then import data:

```powershell
.\scripts\import-main-data.ps1
```

### MongoDB Setup

```powershell
cd mongodb
docker compose up -d
```

### Daily Startup

```powershell
.\scripts\start-oracle.ps1
java -jar target\counterstrike-browser-1.0.0.jar
```

### Build

```powershell
mvn clean package -DskipTests
```

### Milestone 3 Index Analysis

```powershell
.\scripts\run-index-analysis.ps1
```

Results are written to `sql/indexes/*_results.txt`.

## Oracle Connection

`app.properties` (create from `src/main/resources/app.properties.example`):

```properties
db.url=jdbc:oracle:thin:@localhost:1521/FREEPDB1
db.user=CS2
db.password=cs2_password
```
