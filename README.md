# Counter-Strike Database Browser

This project is a Java Swing desktop application backed by an Oracle relational database. It was built for the database project milestones using a Counter-Strike esports domain: players, coaches, teams, tournaments, matches, match records, player match statistics, and tournament participation.

The application connects to Oracle with JDBC, displays structured data through a Swing interface, supports searching and advanced SQL queries, and includes a simple write feature on the Teams page.

## Project Status

Current milestone focus:

- Milestone 2 relational database implementation.
- Oracle schema and integrity constraints.
- Small and large datasets.
- Java Swing application using Oracle JDBC.
- Advanced SQL queries using joins, grouping, sorting, aggregation, and parameterized search.
- Basic write interaction through `Add Team` and `Update Selected`.

Milestone 3 work is not part of this current Java/Oracle implementation yet. Milestone 3 still needs MongoDB support and indexing performance analysis.

## Main Features

- Home dashboard with summary counts and trending panels.
- Players page showing player ID, nickname, full name, rating, ADR, and DPR.
- Teams page showing team region, recent match count, wins, and write controls.
- Matches page showing team-versus-team history, score, tournament, date, winner, and stage.
- Tournaments page showing tournament date range and match counts.
- Search box shared by pages.
- Oracle connection status and row counts in the footer.
- Error dialogs for database, validation, and connection problems.

## Write Feature

The Teams page supports user input that writes to Oracle:

- `Add Team` inserts a new row into `Team`.
- `Update Selected` updates the selected team's `Region`.
- Both operations validate input before saving.
- Both operations use `PreparedStatement`.

This satisfies the Milestone 2 requirement that user input can add or update stored data.

## Advanced SQL Queries

The repository layer contains the required advanced queries in:

`src/main/java/com/counterstrike/app/repository/CounterStrikeRepository.java`

Implemented queries:

- Top players by rating: joins `People` and `Player`, sorted by `Rating DESC`.
- Team match history: joins `MatchTable`, `Team` twice, and `Tournament`.
- Tournament match summary: joins `Tournament` and `MatchTable`, using `COUNT` and `GROUP BY`.
- Player search: parameterized `LIKE` search across player name, nickname, rating, ADR, and DPR.
- Trending team: calculates recent match activity and win counts from `MatchTable`.

All user search values are passed using `PreparedStatement` parameters.

## Technology Stack

- Java Swing for the desktop user interface.
- Java JDBC for database access.
- Oracle Database Free running in Docker for local development.
- Maven for building and packaging.
- Oracle JDBC driver bundled into the final executable jar.
- PowerShell scripts for repeatable setup and data import.

## Important Files

```text
CS2/
  pom.xml
  README.md
  app.properties
  src/main/java/com/counterstrike/app/
  src/main/resources/app.properties.example
  sql/schema.sql
  sql/seed.sql
  Milestone2Files/
    mega_dataset.sql
    smallDataSet.txt
    Project_Milestone2_Counterstrike_all table sql query.txt
    testCodes.txt
  scripts/
    start-oracle.ps1
    import-main-data.ps1
    build.ps1
    run.ps1
  target/
    counterstrike-browser-1.0.0.jar
```

File purpose:

- `pom.xml`: Maven build file and dependencies.
- `src/`: Java source code.
- `sql/schema.sql`: Oracle table creation script aligned with the main milestone dataset.
- `sql/seed.sql`: small demo dataset.
- `Milestone2Files/mega_dataset.sql`: main large dataset.
- `scripts/start-oracle.ps1`: starts the local Oracle Docker container.
- `scripts/import-main-data.ps1`: recreates schema and imports the selected dataset.
- `target/counterstrike-browser-1.0.0.jar`: executable fat jar.
- `app.properties`: local database connection settings. This file should not contain real shared passwords in public submissions.

## Requirements

Each teammate should have:

- Windows with PowerShell.
- Java 17 or newer.
- Maven 3.9 or newer.
- Docker Desktop.
- Enough disk space for the Oracle Docker image and imported dataset.

Check Java:

```powershell
java -version
```

Check Maven:

```powershell
mvn -version
```

Check Docker:

```powershell
docker version
```

If `docker version` cannot connect to the Docker daemon, open Docker Desktop first and wait until it finishes starting.

## Database Configuration

The app reads database settings from `app.properties`.

For the local Docker Oracle setup, use:

```properties
db.url=jdbc:oracle:thin:@localhost:1521/FREEPDB1
db.user=CS2
db.password=cs2_password
```

The project can also use a different config file:

```powershell
java -Dcs2.config=C:\path\to\app.properties -jar target\counterstrike-browser-1.0.0.jar
```

When double-clicking the jar, the app looks for `app.properties` in:

- the current working directory,
- the jar folder,
- the parent folder of the jar folder.

For this project, keeping `app.properties` in the project root is the easiest option.

## First-Time Setup With Oracle Docker

Start Docker Desktop first.

Then run this command once to create the Oracle container:

```powershell
docker run -d --name cs2-oracle -p 1521:1521 -e ORACLE_PASSWORD=oracle_admin_123 -e APP_USER=CS2 -e APP_USER_PASSWORD=cs2_password gvenzl/oracle-free:23-slim-faststart
```

Wait until Oracle finishes first-time startup. It can take several minutes. You can watch logs with:

```powershell
docker logs -f cs2-oracle
```

The database is ready when the logs show:

```text
DATABASE IS READY TO USE!
```

After the container has been created once, do not run `docker run` again. Use the start script instead:

```powershell
.\scripts\start-oracle.ps1
```

The container is configured to use:

- Container name: `cs2-oracle`
- Host port: `1521`
- Oracle service: `FREEPDB1`
- App user: `CS2`
- App password: `cs2_password`

## Importing Data

### Import the Main Large Dataset

Use this for normal milestone demonstration:

```powershell
.\scripts\import-main-data.ps1
```

This script:

- starts Oracle if needed,
- copies `sql/schema.sql` into the container,
- copies `Milestone2Files/mega_dataset.sql` into the container,
- recreates all tables,
- imports the large dataset into user `CS2`.

The large dataset import can take several minutes.

Expected row counts after importing the current main dataset:

```text
People=48
Player=40
Team=8
Tournament=200
MatchTable=20200
MatchRecord=20000
PlayerMatchStats=200000
```

### Import the Small Dataset

For quick testing:

```powershell
.\scripts\import-main-data.ps1 .\Milestone2Files\smallDataSet.txt
```

### Import the Demo Seed Dataset

If you want to use the smaller local `sql/seed.sql` file instead:

```powershell
docker cp sql\schema.sql cs2-oracle:/tmp/schema.sql
docker cp sql\seed.sql cs2-oracle:/tmp/seed.sql
docker exec cs2-oracle bash -lc "sqlplus -L CS2/cs2_password@localhost:1521/FREEPDB1 @/tmp/schema.sql"
docker exec cs2-oracle bash -lc "sqlplus -L CS2/cs2_password@localhost:1521/FREEPDB1 @/tmp/seed.sql"
```

## Build The Application

From the project root:

```powershell
mvn clean package -DskipTests
```

The output jar is:

```text
target/counterstrike-browser-1.0.0.jar
```

This is a fat jar. It includes the Oracle JDBC driver, so teammates do not need to manually download `ojdbc`.

Maven also creates:

```text
target/original-counterstrike-browser-1.0.0.jar
```

Do not use the `original-...` jar for demonstration. It does not include dependencies.

## Run The Application

Make sure Oracle is running:

```powershell
.\scripts\start-oracle.ps1
```

Then run:

```powershell
java -jar target\counterstrike-browser-1.0.0.jar
```

You can also run through Maven:

```powershell
mvn exec:java
```

Double-clicking `target/counterstrike-browser-1.0.0.jar` should also work if Java is installed and `app.properties` is in a discoverable location.

## Fallback Build Without Maven

The project also includes simple PowerShell scripts for compiling with `javac`:

```powershell
.\scripts\build.ps1
.\scripts\run.ps1
```

Use Maven for the proper project build. The fallback scripts are only for quick local checks.

## Suggested Demo Flow

Before the lab presentation:

1. Open Docker Desktop.
2. Run `.\scripts\start-oracle.ps1`.
3. If needed, run `.\scripts\import-main-data.ps1`.
4. Run `mvn clean package -DskipTests`.
5. Run `java -jar target\counterstrike-browser-1.0.0.jar`.
6. Keep the app open before presenting.

During the demo:

1. Show Home summary counts and trending panels.
2. Open Players and search a nickname such as `donk`.
3. Open Teams and show recent match count and wins.
4. Use `Add Team` to insert a test team.
5. Select the inserted team and use `Update Selected` to change its region.
6. Open Matches and search a team such as `MOUZ`.
7. Open Tournaments and show match count aggregation.
8. Explain that advanced queries are implemented with joins, grouping, and prepared statements.

If you insert a test team during practice, re-import the dataset before the final demo if you want the database reset:

```powershell
.\scripts\import-main-data.ps1
```

## Useful Verification Queries

Run these inside the Oracle container:

```powershell
docker exec -it cs2-oracle bash
sqlplus CS2/cs2_password@localhost:1521/FREEPDB1
```

Then run:

```sql
SELECT COUNT(*) FROM People;
SELECT COUNT(*) FROM Player;
SELECT COUNT(*) FROM Team;
SELECT COUNT(*) FROM Tournament;
SELECT COUNT(*) FROM MatchTable;
SELECT COUNT(*) FROM MatchRecord;
SELECT COUNT(*) FROM PlayerMatchStats;
```

Exit SQLPlus:

```sql
EXIT;
```

## Troubleshooting

### ORA-12541: No Listener

Meaning: the app cannot reach an Oracle listener on `localhost:1521`.

Fix:

```powershell
.\scripts\start-oracle.ps1
```

If Docker is not running, open Docker Desktop first.

### ORA-01017: Invalid Username/Password

Check `app.properties`:

```properties
db.user=CS2
db.password=cs2_password
```

Also make sure the Oracle container was created with:

```text
APP_USER=CS2
APP_USER_PASSWORD=cs2_password
```

### Table Or Column Not Found

The database schema may not match the current Java code. Re-import:

```powershell
.\scripts\import-main-data.ps1
```

### Docker Container Already Exists

If you accidentally try to run the `docker run` command again, Docker may report that `cs2-oracle` already exists.

Use:

```powershell
docker start cs2-oracle
```

or:

```powershell
.\scripts\start-oracle.ps1
```

### Jar Opens But Shows A Database Error

The jar is working, but the database is not reachable or the config is wrong. Check:

- Docker Desktop is running.
- `cs2-oracle` is running.
- `app.properties` exists.
- `db.url` points to `localhost:1521/FREEPDB1`.
- `db.user` and `db.password` are correct.

### Wrong Jar

Use:

```text
target/counterstrike-browser-1.0.0.jar
```

Do not use:

```text
target/original-counterstrike-browser-1.0.0.jar
```

## Notes For The Report

Important evidence for Milestone 2:

- Screenshot of the ER diagram and relational schema.
- Screenshot of Oracle tables after small dataset import.
- Screenshot of row counts after large dataset import.
- Screenshot of the Swing app Home page.
- Screenshot of Players search.
- Screenshot of Team add/update.
- Screenshot of Matches history.
- Screenshot of Tournament match summary.
- Description of advanced SQL queries.
- Explanation that user input is parameterized with `PreparedStatement`.

Large dataset explanation:

- The project uses a realistic small set of esports entities such as teams and players.
- The large dataset scales high-volume event tables such as `MatchTable`, `MatchRecord`, and `PlayerMatchStats`.
- This is reasonable because real esports systems have a limited number of teams and players compared with many match records and per-player statistics.

## Submission Guidance

For Cadmus, the report is the main submission. The code and scripts are supporting files.

Recommended attachments:

- Full project zip.
- `sql/schema.sql`
- `sql/seed.sql`
- `Milestone2Files/mega_dataset.sql`
- `pom.xml`
- `target/counterstrike-browser-1.0.0.jar`
- `scripts/start-oracle.ps1`
- `scripts/import-main-data.ps1`
- Screenshots used in the report.

For final presentation, do not rely on the marker setting up the database during the session. Have Docker, Oracle, and the Java app open and ready before the presentation starts.
