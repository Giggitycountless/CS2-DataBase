# CS2 Database Browser — Full Technical Documentation

This document explains every key class and method in the project in detail: what it does,
why it works, and how the pieces connect. Topics covered include database connectivity,
metadata-driven SQL generation, search mechanics, CRUD operations, column filtering,
and the Swing UI threading model.

---

## Table of Contents

1. [Project Architecture Overview](#1-project-architecture-overview)
2. [Application Entry Point — CounterStrikeApp](#2-application-entry-point--counterstrikeapp)
3. [Configuration Layer — DatabaseConfig](#3-configuration-layer--databaseconfig)
4. [Database Connection Layer](#4-database-connection-layer)
   - 4.1 [Database.java (Oracle / JDBC)](#41-databasejava-oracle--jdbc)
   - 4.2 [MongoDatabase.java (MongoDB)](#42-mongodatabasejava-mongodb)
5. [Data Contract — AppRepository Interface](#5-data-contract--apprepository-interface)
6. [Metadata Layer — Schema, TableSpec, ColumnSpec](#6-metadata-layer--schema-tablespec-columnspec)
   - 6.1 [ColumnSpec — Describing a Single Column](#61-columnspec--describing-a-single-column)
   - 6.2 [TableSpec — Describing a Table](#62-tablespec--describing-a-table)
   - 6.3 [Schema — The Full Table Catalogue](#63-schema--the-full-table-catalogue)
7. [Data Transfer Objects — TableData, HomeSummary](#7-data-transfer-objects--tabledata-homesummary)
8. [Oracle Implementation — CounterStrikeRepository](#8-oracle-implementation--counterstrikerepository)
   - 8.1 [How Search Works](#81-how-search-works)
   - 8.2 [How Browse Works (Metadata-Driven SELECT)](#82-how-browse-works-metadata-driven-select)
   - 8.3 [How INSERT Works](#83-how-insert-works)
   - 8.4 [How UPDATE Works](#84-how-update-works)
   - 8.5 [How DELETE Works](#85-how-delete-works)
   - 8.6 [distinctValues — Populating Dropdowns](#86-distinctvalues--populating-dropdowns)
   - 8.7 [Transaction Safety in executeWrite](#87-transaction-safety-in-executewrite)
   - 8.8 [SQL Injection Prevention](#88-sql-injection-prevention)
   - 8.9 [Analytics Queries](#89-analytics-queries)
9. [MongoDB Implementation — MongoCounterStrikeRepository](#9-mongodb-implementation--mongocounterstrikerepository)
   - 9.1 [Document Schema Design](#91-document-schema-design)
   - 9.2 [How Browse Works (Document Scan + Field Extraction)](#92-how-browse-works-document-scan--field-extraction)
   - 9.3 [How Search Works](#93-how-search-works)
   - 9.4 [Field Mapping: Oracle Names → MongoDB Names](#94-field-mapping-oracle-names--mongodb-names)
   - 9.5 [Embedded Arrays — expandRecords, expandPlayerStats](#95-embedded-arrays--expandrecords-expandplayerstats)
   - 9.6 [Write Operations — INSERT / UPDATE / DELETE](#96-write-operations--insert--update--delete)
   - 9.7 [distinctValues — The Codec Problem and Its Fix](#97-distinctvalues--the-codec-problem-and-its-fix)
10. [Analytics Layer — AnalyticsQuery, AnalyticsCatalog](#10-analytics-layer--analyticsquery-analyticscatalog)
11. [UI Layer](#11-ui-layer)
    - 11.1 [MainFrame — Window Shell and Navigation](#111-mainframe--window-shell-and-navigation)
    - 11.2 [SwingWorker — Background Threading Model](#112-swingworker--background-threading-model)
    - 11.3 [ReadOnlyTableModel — Feeding Data to JTable](#113-readonlytablemodel--feeding-data-to-jtable)
    - 11.4 [Styles — Shared Widget Factory](#114-styles--shared-widget-factory)
    - 11.5 [CrudPanel — The Manage Page](#115-crudpanel--the-manage-page)
    - 11.6 [AnalyticsPanel — The Analytics Page](#116-analyticspanel--the-analytics-page)
12. [End-to-End Data Flows](#12-end-to-end-data-flows)
    - 12.1 [Searching for a Player](#121-searching-for-a-player)
    - 12.2 [Adding a New Row](#122-adding-a-new-row)
    - 12.3 [Editing an Existing Row](#123-editing-an-existing-row)
    - 12.4 [Deleting a Row](#124-deleting-a-row)
    - 12.5 [Column Filter (Client-Side)](#125-column-filter-client-side)
    - 12.6 [Switching Databases at Runtime](#126-switching-databases-at-runtime)
13. [Design Principles Summary](#13-design-principles-summary)

---

## 1. Project Architecture Overview

The application is organized into four distinct layers that communicate strictly top-to-bottom:

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│  MainFrame  CrudPanel  AnalyticsPanel  Styles           │
│  (Swing / EDT)                                          │
└───────────────────────┬─────────────────────────────────┘
                        │  calls
                        ▼
┌─────────────────────────────────────────────────────────┐
│              Data Contract (Interface)                  │
│                  AppRepository                          │
└──────────┬─────────────────────────────┬────────────────┘
           │ implements                  │ implements
           ▼                             ▼
┌──────────────────────┐   ┌──────────────────────────────┐
│ CounterStrike        │   │ MongoCounterStrike            │
│ Repository           │   │ Repository                   │
│ (Oracle / JDBC)      │   │ (MongoDB Driver)             │
└──────────┬───────────┘   └──────────────┬───────────────┘
           │ uses                          │ uses
           ▼                              ▼
┌──────────────────────┐   ┌──────────────────────────────┐
│   Database.java      │   │   MongoDatabase.java         │
│   (JDBC connection)  │   │   (MongoClient pool)         │
└──────────────────────┘   └──────────────────────────────┘
```

**Key architectural principle — Interface Isolation:**
The UI layer holds a reference to `AppRepository` (the interface), never to a concrete
class. This means the UI does not know or care whether data comes from Oracle or MongoDB.
Swapping the backend at runtime only requires passing a different implementation object
to `MainFrame`.

---

## 2. Application Entry Point — CounterStrikeApp

**File:** `src/main/java/com/counterstrike/app/CounterStrikeApp.java`

### `main()` — JVM entry point

```java
public static void main(String[] args) {
    FlatDarkLaf.setup();                     // Install the dark look-and-feel
    UIManager.put("Button.arc", 8);          // Rounded button corners
    UIManager.put("ScrollBar.thumbArc", 999);// Circular scrollbar thumb
    // ... more FlatLaf tweaks ...
    SwingUtilities.invokeLater(() -> connect(null));
}
```

`SwingUtilities.invokeLater()` is mandatory in Java Swing. Swing is **not thread-safe**:
all UI construction and manipulation must happen on the **Event Dispatch Thread (EDT)**.
`invokeLater` schedules the lambda to run on the EDT after the JVM finishes initializing.
Passing `null` as `oldFrame` signals this is the initial startup (no existing window to close).

### `connect(JFrame oldFrame)` — Connection dialog and wiring

```java
public static void connect(javax.swing.JFrame oldFrame) {
    // Step 1: Show a modal dialog asking which backend to use
    String[] options = {"Oracle (Relational)", "MongoDB (NoSQL)"};
    int choice = JOptionPane.showOptionDialog(oldFrame, ...);

    if (choice < 0) return;   // User dismissed the dialog — do nothing

    try {
        AppRepository repository;
        String dbInfo;

        if (choice == 0) {
            // --- Oracle path ---
            DatabaseConfig config = DatabaseConfig.load(); // Read app.properties
            Database database = new Database(config);
            database.testConnection();                     // Fail fast if unreachable
            repository = new CounterStrikeRepository(database);
            dbInfo = config.url();

        } else {
            // --- MongoDB path ---
            String uri = (String) JOptionPane.showInputDialog(...); // Prompt for URI
            if (uri == null || uri.isBlank()) return;
            MongoDatabase mongo = new MongoDatabase(uri.trim(), "cs_small");
            mongo.testConnection();                        // Sends a ping command
            repository = new MongoCounterStrikeRepository(mongo);
            dbInfo = uri.trim() + "/cs2";
        }

        // Step 2: Build the UI, passing the chosen repository
        MainFrame frame = new MainFrame(repository, dbInfo);
        frame.setVisible(true);

        // Step 3: If this was a database switch, close the old window
        if (oldFrame != null) oldFrame.dispose();

    } catch (Exception exception) {
        // Show the error. If connection failed, oldFrame is still alive and usable.
        JOptionPane.showMessageDialog(oldFrame, exception.getMessage(), ...);
    }
}
```

**Why `public static`?**
The "Switch DB" button in `MainFrame` calls `CounterStrikeApp.connect(MainFrame.this)`.
Because `MainFrame` is in a different package (`ui`), the method must be `public` for it
to be visible. Making it `static` avoids needing a `CounterStrikeApp` instance — it is
a pure factory/utility method.

**Fail-fast design:** `testConnection()` is called before creating `MainFrame`. If the
database is unreachable, the exception is caught immediately and the error dialog is shown.
No half-built window is ever displayed.

---

## 3. Configuration Layer — DatabaseConfig

**File:** `src/main/java/com/counterstrike/app/config/DatabaseConfig.java`

```java
public record DatabaseConfig(String url, String user, String password) {
    public static DatabaseConfig load() { ... }
}
```

`DatabaseConfig` is a Java **record** — an immutable data carrier. Its three fields
(`url`, `user`, `password`) map to keys in `app.properties`:

```
db.url      = jdbc:oracle:thin:@localhost:1521/FREE
db.user     = system
db.password = oracle
```

### Config resolution order (in `findConfigPath()`)

The loader tries four locations in order, stopping at the first one that exists:

1. **System property** `-Dcs2.config=C:\path\to\app.properties`
   (passed on the command line, e.g. `java -Dcs2.config=... -jar app.jar`)
2. **Environment variable** `CS2_DB_CONFIG`
3. **Working directory** `./app.properties` (next to the JAR)
4. **Classpath** `/app.properties` embedded inside the JAR (the default fallback)

This layered lookup lets the same JAR work in development (file on disk), CI (environment
variable), and as a standalone release (bundled properties) without changing any code.

---

## 4. Database Connection Layer

### 4.1 Database.java (Oracle / JDBC)

**File:** `src/main/java/com/counterstrike/app/db/Database.java`

```java
public final class Database {
    private final DatabaseConfig config;

    public Connection getConnection() throws SQLException {
        loadOracleDriverIfPresent();
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    public void testConnection() throws SQLException {
        try (Connection ignored = getConnection()) {
            // Open and immediately close — validates URL, credentials, driver
        }
    }

    private void loadOracleDriverIfPresent() {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ignored) {
            // JDBC 4+ drivers register themselves via ServiceLoader; this is a fallback
        }
    }
}
```

**Important design choice — no connection pool:**
`getConnection()` opens a **brand new JDBC connection every time it is called**. Each
repository method calls `getConnection()`, uses the connection, then closes it in a
`try-with-resources` block. This is simple and correct for a single-user desktop
application. A production server would use a connection pool (HikariCP, c3p0, etc.) to
reuse connections, but here the overhead is acceptable and the simplicity is valuable.

**`loadOracleDriverIfPresent()`:** JDBC 4.0 (Java 6+) introduced automatic driver
discovery via `java.util.ServiceLoader`. The driver JAR includes a file at
`META-INF/services/java.sql.Driver` that registers it automatically. The explicit
`Class.forName()` call is a belt-and-suspenders approach for older environments.

### 4.2 MongoDatabase.java (MongoDB)

**File:** `src/main/java/com/counterstrike/app/db/MongoDatabase.java`

```java
public final class MongoDatabase {
    private final String uri;
    private final String dbName;
    private MongoClient client;   // null until first use

    public com.mongodb.client.MongoDatabase getDatabase() {
        if (client == null) {
            client = MongoClients.create(uri);  // Lazy initialization
        }
        return client.getDatabase(dbName);
    }

    public void testConnection() {
        getDatabase().runCommand(new Document("ping", 1));
    }

    public void close() {
        if (client != null) {
            client.close();   // Shuts down the connection pool and all background threads
            client = null;
        }
    }
}
```

**Lazy initialization:** `MongoClients.create(uri)` creates a connection pool (multiple
sockets, heartbeat threads, server-monitoring threads). This is expensive, so it is
deferred until the first actual database call, not done at construction time.

**`getDatabase(dbName)`** does not open a network connection. It merely returns a
`MongoDatabase` handle object. The actual network I/O happens when a collection is
accessed and a command is executed.

**`testConnection()` sends a `ping`:** MongoDB's `ping` command is a minimal server
round-trip that returns `{"ok": 1}` if the server is reachable and the credentials are
valid.

**`close()` is critical for the Switch DB feature:** When the user switches databases,
the old `MongoDatabase` must be closed to release the background thread pool and socket
connections. If `close()` is never called, each switch leaks a `MongoClient` and its
associated system resources until the JVM exits.

---

## 5. Data Contract — AppRepository Interface

**File:** `src/main/java/com/counterstrike/app/repository/AppRepository.java`

```java
public interface AppRepository {
    // ── Home page queries ──────────────────────────────────────────────────
    TableData findTopPlayers(String search)            throws Exception;
    TableData findTrendingTeams(String search)         throws Exception;
    TableData findTeamMatchHistory(String search)      throws Exception;
    TableData findTournamentMatchSummary(String search) throws Exception;
    TableData findPeople(String search)                throws Exception;
    TableData findTeamsBasic(String search)            throws Exception;
    TableData findTournamentsBasic(String search)      throws Exception;

    // ── Home page summary ─────────────────────────────────────────────────
    HomeSummary loadHomeSummary()                      throws Exception;

    // ── Generic CRUD (Manage page) ────────────────────────────────────────
    TableData browse(TableSpec spec, String search)    throws Exception;
    List<String> distinctValues(String table, String column) throws Exception;
    void insertRow(TableSpec spec, Map<String, String> rawValues) throws Exception;
    void updateRow(TableSpec spec, Map<String, String> primaryKey,
                   Map<String, String> rawValues)      throws Exception;
    void deleteRow(TableSpec spec, Map<String, String> primaryKey) throws Exception;

    // ── Analytics ─────────────────────────────────────────────────────────
    TableData runAnalytics(AnalyticsQuery query, String parameter) throws Exception;
    boolean supportsAnalytics();

    // ── Meta ──────────────────────────────────────────────────────────────
    String dbLabel();   // Returns "Oracle" or "MongoDB" for the status bar
}
```

Every method throws `Exception` (the widest checked type) rather than a specific
exception. This is intentional: Oracle throws `SQLException`, MongoDB throws
`MongoException`. Using `Exception` means the UI layer does not need to import or know
about driver-specific exception classes — it just catches `Exception` and extracts the
message.

---

## 6. Metadata Layer — Schema, TableSpec, ColumnSpec

This layer is the heart of the "write once, run everywhere" CRUD system. Every table in
the database is described as a Java object. The repository uses those descriptions to
generate SQL automatically, and the UI uses them to generate forms automatically.

### 6.1 ColumnSpec — Describing a Single Column

**File:** `src/main/java/com/counterstrike/app/repository/ColumnSpec.java`

```java
public record ColumnSpec(
    String name,          // Exact database column name, e.g. "PersonID"
    String label,         // Human-readable label shown in the UI, e.g. "Person ID"
    Type type,            // TEXT | INTEGER | DECIMAL | DATE
    boolean primaryKey,   // True → this column is part of the table's PK
    boolean required,     // True → the UI marks it with * and validates non-blank
    ForeignKey foreignKey // Non-null → UI renders an editable dropdown
) {
    public enum Type { TEXT, INTEGER, DECIMAL, DATE }

    public record ForeignKey(String table, String column) {}

    public boolean isDate() { return type == Type.DATE; }
}
```

**Factory methods** hide the verbose constructor:

| Method | Purpose |
|---|---|
| `pk(name, label, type)` | Primary key column (primaryKey=true, required=true, no FK) |
| `pkRef(name, label, type, table, col)` | PK that is also an FK (e.g. Player.PersonID → People.PersonID) |
| `col(name, label, type)` | Optional, non-key, non-FK column |
| `required(name, label, type)` | Mandatory non-key column |
| `ref(name, label, type, table, col, required)` | Non-key column with an FK reference |

**How `ForeignKey` drives dropdown menus:**
When `CrudPanel` opens an Add/Edit form, it calls `repository.distinctValues(fk.table(),
fk.column())` for every column whose `foreignKey` is non-null. The result is loaded into
an **editable** `JComboBox`, so users see existing values but can type new ones freely.

**The Nationality self-reference:**
```java
ref("Nationality", "Nationality", Type.TEXT, "People", "Nationality", false)
```
This tells `CrudPanel` to call `distinctValues("People", "Nationality")`, which runs
`SELECT DISTINCT Nationality FROM People`. The result is the list of nationalities already
in the database, shown as suggestions. Since `JComboBox.setEditable(true)` is called, a
user can still type any new nationality — there is no hard enforcement.

### 6.2 TableSpec — Describing a Table

**File:** `src/main/java/com/counterstrike/app/repository/TableSpec.java`

```java
public record TableSpec(
    String displayName,      // Shown in the table selector, e.g. "Player Contract"
    String tableName,        // Real SQL table name, e.g. "PlayerContract"
    List<ColumnSpec> columns,
    String orderBy           // Default ORDER BY clause
) {
    public List<ColumnSpec> primaryKey()       // → columns where primaryKey == true
    public List<ColumnSpec> insertableColumns() // → all columns (used for INSERT)
    public List<ColumnSpec> editableColumns()   // → non-PK columns (used for UPDATE SET)
    public String[] columnLabels()              // → array of label strings for JTable headers
}
```

`insertableColumns()` returns ALL columns because when inserting a new row, you supply
values for every field including the PK. `editableColumns()` returns only non-PK columns
because you cannot change a primary key — it identifies the row in the WHERE clause.

**`orderBy`** is a raw SQL fragment appended to every `browse()` SELECT. For example:

```java
// Tournament table — newest first, then by ID for stability
new TableSpec("Tournament", "Tournament", ...,
              "TournyStartDate DESC NULLS LAST, TournamentID DESC")
```

`NULLS LAST` is Oracle syntax that sorts rows with NULL dates at the end rather than first.

### 6.3 Schema — The Full Table Catalogue

**File:** `src/main/java/com/counterstrike/app/repository/Schema.java`

```java
public static final List<TableSpec> ALL = List.of(
    PEOPLE, PLAYER, COACH, TEAM,
    PLAYER_CONTRACT, COACH_CONTRACT,
    TOURNAMENT, MATCH, MATCH_RECORD,
    PLAYER_MATCH_STATS, TOURNAMENT_PARTICIPATION
);
```

`Schema.ALL` is iterated by `CrudPanel` to populate the table selector dropdown. Because
`TableSpec` implements `toString()` via its `displayName`, the `JComboBox` automatically
shows the friendly name.

**Why this design is powerful:**
Adding a new table to the application requires exactly **one change**: adding a new
`TableSpec` constant to `Schema.java`. The `browse()`, `insertRow()`, `updateRow()`,
`deleteRow()` methods in the repository will automatically handle it. The `CrudPanel`
will automatically add it to the table selector and render the correct form.

---

## 7. Data Transfer Objects — TableData, HomeSummary

### TableData

```java
public record TableData(String[] columns, List<Object[]> rows) {
    public int rowCount() { return rows.size(); }
}
```

A minimal container passed from the repository to the UI. `columns` holds the header
labels; `rows` holds one `Object[]` per row, where each element is the raw value
retrieved from the database (`Long`, `BigDecimal`, `java.sql.Date`, `String`, etc.).
`ReadOnlyTableModel` adapts this to Swing's `TableModel` API.

### HomeSummary

```java
public record HomeSummary(
    int playerCount, int teamCount, int matchCount, int tournamentCount,
    String topPlayerName, String topPlayerDetail,
    String trendingTeamName, String trendingTeamDetail
)
```

Used exclusively by the Home page to populate the four metric cards and two trending
cards. Computed by `loadHomeSummary()`, which calls `findTopPlayers("")` and
`findTrendingTeams("")` and then counts rows via `COUNT(*)` queries.

---

## 8. Oracle Implementation — CounterStrikeRepository

**File:** `src/main/java/com/counterstrike/app/repository/CounterStrikeRepository.java`

### 8.1 How Search Works

Every `find*()` method follows the same pattern. Here is `findTopPlayers()` explained
step by step:

```java
public TableData findTopPlayers(String search) throws SQLException {
    String sql = """
        SELECT p.PersonID, p.Nickname, p.FullName, pl.Rating, pl.ADR, pl.DPR
        FROM People p
        JOIN Player pl ON p.PersonID = pl.PersonID
        WHERE LOWER(p.FullName) LIKE ?
           OR LOWER(NVL(p.Nickname, '')) LIKE ?
           OR TO_CHAR(pl.Rating) LIKE ?
           OR TO_CHAR(pl.ADR)    LIKE ?
           OR TO_CHAR(pl.DPR)    LIKE ?
        ORDER BY pl.Rating DESC NULLS LAST, p.FullName
        """;
    String textLike = like(search);   // "s1mple" → "%s1mple%"
    return queryTable(
        new String[]{"PlayerID","Nickname","Name","Rating","ADR","DPR"},
        sql,
        statement -> {
            statement.setString(1, textLike);
            statement.setString(2, textLike);
            statement.setString(3, numberLike(search)); // matches "1.0" or "1"
            statement.setString(4, numberLike(search));
            statement.setString(5, numberLike(search));
        }
    );
}
```

**`like(String search)`** converts the raw user input:
- Empty string → `"%"` (matches everything — `WHERE col LIKE '%'` is always true)
- Otherwise → `"%" + search.trim().toLowerCase() + "%"`

**`numberLike(String search)`** is the same but also strips trailing `.0` to ensure that
searching "1" finds values stored as "1.0":
```java
private String numberLike(String search) {
    String s = normalize(search);  // trim + lowercase
    if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
    return "%" + s + "%";
}
```

**`LOWER(p.FullName) LIKE ?`:** Lowercasing both the column value and the search term
makes the match case-insensitive without needing Oracle's `UPPER()` or a case-insensitive
collation.

**`NVL(p.Nickname, '')`:** `NVL` is Oracle's null-coalescing function. Without it,
`LOWER(NULL) LIKE '%s1mple%'` evaluates to `NULL` (not `FALSE`), meaning rows with a
NULL nickname would never match — even if the user is not searching for anything specific.
Wrapping with `NVL(..., '')` converts NULL to an empty string so the `LIKE` comparison
works correctly.

**`TO_CHAR(pl.Rating)`:** Rating is a `NUMBER` column. Oracle cannot apply `LIKE` to a
number, so it is cast to a string first. This allows the search bar to match "1.05" by
typing "1" or "05".

### 8.2 How Browse Works (Metadata-Driven SELECT)

`browse()` is the engine of the Manage page. It generates a complete SELECT statement
from a `TableSpec` without any table-specific code:

```java
public TableData browse(TableSpec spec, String search) throws SQLException {
    List<ColumnSpec> columns = spec.columns();

    // ── Build SELECT clause from column names ─────────────────────────────
    StringJoiner select = new StringJoiner(", ");
    for (ColumnSpec column : columns) {
        select.add(column.name());
    }
    // Result: "PersonID, Nickname, FullName, Birthday, Nationality"

    // ── Build WHERE clause — each column gets one LIKE condition ──────────
    StringBuilder sql = new StringBuilder("SELECT ").append(select)
            .append(" FROM ").append(spec.tableName());

    String trimmed = normalize(search);
    boolean filtered = !trimmed.isEmpty();
    if (filtered) {
        StringJoiner where = new StringJoiner(" OR ", " WHERE ", "");
        for (ColumnSpec column : columns) {
            if (column.isDate()) {
                // Dates must be formatted as text before LIKE can match them
                where.add("TO_CHAR(" + column.name() + ", 'YYYY-MM-DD') LIKE ?");
            } else {
                where.add("LOWER(TO_CHAR(" + column.name() + ")) LIKE ?");
            }
        }
        sql.append(where);
    }

    // ── Append ORDER BY from TableSpec metadata ────────────────────────────
    sql.append(" ORDER BY ").append(spec.orderBy());

    // ── Bind one parameter per column ─────────────────────────────────────
    String textLike = like(trimmed);
    String dateLike = numberLike(trimmed);    // Dates stored as numbers don't need %
    return queryTable(spec.columnLabels(), sql.toString(), statement -> {
        if (filtered) {
            int index = 1;
            for (ColumnSpec column : columns) {
                statement.setString(index++, column.isDate() ? dateLike : textLike);
            }
        }
    });
}
```

**Example: browsing the People table with search "russia"**

Generated SQL:
```sql
SELECT PersonID, Nickname, FullName, Birthday, Nationality
FROM People
WHERE LOWER(TO_CHAR(PersonID)) LIKE '%russia%'
   OR LOWER(TO_CHAR(Nickname)) LIKE '%russia%'
   OR LOWER(TO_CHAR(FullName)) LIKE '%russia%'
   OR TO_CHAR(Birthday, 'YYYY-MM-DD') LIKE '%russia%'
   OR LOWER(TO_CHAR(Nationality)) LIKE '%russia%'
ORDER BY PersonID
```

Only rows where any column contains "russia" (case-insensitive) are returned. Since the
Nationality column will contain "Russia", those rows match.

**Why `TO_CHAR(column)` everywhere?** Oracle's `LOWER()` and `LIKE` only work on string
types. `TO_CHAR()` converts dates and numbers to strings so the same generic WHERE
template works for every column regardless of its data type.

### 8.3 How INSERT Works

```java
public void insertRow(TableSpec spec, Map<String, String> rawValues) throws SQLException {
    List<ColumnSpec> columns = spec.insertableColumns();  // All columns

    // Build column list and placeholder list
    StringJoiner names = new StringJoiner(", ");
    StringJoiner marks = new StringJoiner(", ");
    for (ColumnSpec column : columns) {
        names.add(column.name());
        marks.add("?");
    }

    // Example result: INSERT INTO People (PersonID, Nickname, FullName, Birthday, Nationality)
    //                 VALUES (?, ?, ?, ?, ?)
    String sql = "INSERT INTO " + spec.tableName()
               + " (" + names + ") VALUES (" + marks + ")";

    executeWrite(sql, statement -> {
        int index = 1;
        for (ColumnSpec column : columns) {
            bind(statement, index++, column, rawValues.get(column.name()));
        }
    });
}
```

**`bind(statement, index, column, rawValue)`** converts the String value from the form
into the correct Java/JDBC type based on `column.type()`:

```java
private void bind(PreparedStatement stmt, int i, ColumnSpec col, String raw)
        throws SQLException {
    if (raw == null || raw.isBlank()) {
        stmt.setNull(i, Types.NULL);
        return;
    }
    switch (col.type()) {
        case INTEGER -> stmt.setLong(i, Long.parseLong(raw.trim()));
        case DECIMAL -> stmt.setBigDecimal(i, new BigDecimal(raw.trim()));
        case DATE    -> {
            LocalDate d = LocalDate.parse(raw.trim()); // Parses "2003-05-15"
            stmt.setDate(i, java.sql.Date.valueOf(d));
        }
        case TEXT    -> stmt.setString(i, raw.trim());
    }
}
```

This type-safe binding ensures that "1.05" typed by the user becomes a `BigDecimal(1.05)`
in the database rather than a literal string "1.05", and "2003-05-15" becomes a `DATE`
column value rather than a string.

### 8.4 How UPDATE Works

```java
public void updateRow(TableSpec spec,
                      Map<String, String> primaryKey,
                      Map<String, String> rawValues) throws SQLException {

    List<ColumnSpec> editable = spec.editableColumns();  // Non-PK columns only
    List<ColumnSpec> keys     = spec.primaryKey();

    // Build: UPDATE People SET Nickname = ?, FullName = ?, Birthday = ?, Nationality = ?
    StringJoiner set = new StringJoiner(", ");
    for (ColumnSpec col : editable) set.add(col.name() + " = ?");

    // Build: WHERE PersonID = ?
    StringJoiner where = new StringJoiner(" AND ");
    for (ColumnSpec key : keys) where.add(key.name() + " = ?");

    String sql = "UPDATE " + spec.tableName() + " SET " + set + " WHERE " + where;

    int affected = executeWrite(sql, statement -> {
        int index = 1;
        // Bind SET values first
        for (ColumnSpec col : editable) {
            bind(statement, index++, col, rawValues.get(col.name()));
        }
        // Then bind WHERE values (primary key from the selected row)
        for (ColumnSpec key : keys) {
            bind(statement, index++, key, primaryKey.get(key.name()));
        }
    });

    if (affected == 0) {
        throw new IllegalArgumentException("No matching row was found to update.");
    }
}
```

`primaryKey` is a `Map<String, String>` populated by `CrudPanel` from the **currently
selected table row** before opening the edit form. It contains the original PK values, so
the WHERE clause uniquely identifies the exact row to update.

`affected == 0` means the row disappeared between when the user selected it and when they
submitted the form (e.g., deleted by another session). The error is surfaced to the user.

### 8.5 How DELETE Works

```java
public void deleteRow(TableSpec spec, Map<String, String> primaryKey) throws SQLException {
    List<ColumnSpec> keys = spec.primaryKey();
    StringJoiner where = new StringJoiner(" AND ");
    for (ColumnSpec key : keys) {
        where.add(key.name() + " = ?");
    }
    // Example: DELETE FROM People WHERE PersonID = ?
    String sql = "DELETE FROM " + spec.tableName() + " WHERE " + where;

    int affected = executeWrite(sql, statement -> {
        int index = 1;
        for (ColumnSpec key : keys) {
            bind(statement, index++, key, primaryKey.get(key.name()));
        }
    });

    if (affected == 0) {
        throw new IllegalArgumentException("No matching row was found to delete.");
    }
}
```

The WHERE clause is built from all PK columns using `AND`. For compound primary keys
(e.g., `PlayerMatchStats` has `MatchRecordID + PlayerID`), both conditions must match.

### 8.6 distinctValues — Populating Dropdowns

```java
public List<String> distinctValues(String table, String column) throws SQLException {
    String sql = "SELECT DISTINCT " + column
               + " FROM " + table
               + " WHERE " + column + " IS NOT NULL"
               + " ORDER BY " + column;
    List<String> values = new ArrayList<>();
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
            values.add(String.valueOf(resultSet.getObject(1)));
        }
    }
    return values;
}
```

`SELECT DISTINCT` returns each unique value exactly once. Oracle processes this on the
server, so only the distinct values are transmitted — not all rows. The `IS NOT NULL`
filter prevents null entries from appearing in the dropdown.

**Example — Nationality dropdown:**
```sql
SELECT DISTINCT Nationality FROM People
WHERE Nationality IS NOT NULL
ORDER BY Nationality
-- Returns: ["Australia", "Denmark", "France", "Russia", ...]
```

These become the items in the `JComboBox`. Because `setEditable(true)` is called on the
combo, the user can type a new nationality not yet in the list.

### 8.7 Transaction Safety in executeWrite

```java
private int executeWrite(String sql, StatementBinder binder) throws SQLException {
    try (Connection connection = database.getConnection()) {
        connection.setAutoCommit(false);   // Begin explicit transaction
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            int affected = statement.executeUpdate();
            connection.commit();           // Persist the change
            return affected;
        } catch (SQLException exception) {
            safeRollback(connection);      // Undo on any SQL error
            throw new SQLException(friendlyMessage(exception), exception);
        } catch (RuntimeException exception) {
            safeRollback(connection);      // Undo on programming errors too
            throw exception;
        }
    }
}
```

By default, JDBC connections have `autoCommit = true`, meaning each statement is
committed immediately. Setting `autoCommit(false)` enables an **explicit transaction**:

- If `executeUpdate()` succeeds → `commit()` makes the change permanent.
- If any exception occurs → `rollback()` undoes all changes made in this connection
  (important if a future enhancement adds multi-statement operations).

`friendlyMessage(exception)` translates Oracle error codes into readable text:
```java
private String friendlyMessage(SQLException e) {
    return switch (e.getErrorCode()) {
        case 1     -> "A record with this ID already exists (unique constraint).";
        case 2291  -> "Referenced record does not exist (foreign key constraint).";
        case 2292  -> "Cannot delete — other records depend on this one.";
        case 1400  -> "A required field is missing (NOT NULL constraint).";
        default    -> e.getMessage();
    };
}
```

Oracle error 2291 (ORA-02291) means "parent key not found" — the user tried to insert
a PlayerContract referencing a PersonID that does not exist in People.

### 8.8 SQL Injection Prevention

All user-supplied values go through JDBC **PreparedStatement** with `?` placeholders,
never through string concatenation. This is the fundamental defense against SQL injection.

```java
// SAFE — user input is bound as a value, never interpreted as SQL
statement.setString(1, userInput);

// UNSAFE — never done in this codebase
String sql = "SELECT * FROM People WHERE Name = '" + userInput + "'";
```

If `userInput` were `' OR '1'='1`, the safe version passes it as a literal string to
Oracle. The unsafe version would turn it into `WHERE Name = '' OR '1'='1'`, returning
all rows.

**Column and table names** in `browse()`, `insertRow()`, `updateRow()`, `deleteRow()` are
taken directly from `Schema.java` (a compile-time constant), not from user input. So
there is no injection risk there either.

### 8.9 Analytics Queries

```java
public TableData runAnalytics(AnalyticsQuery query, String parameter) throws SQLException {
    String value = like(parameter);                        // "%param%" or "%"
    int placeholders = query.placeholderCount();           // Count "?" in the SQL
    try (Connection connection = database.getConnection();
         PreparedStatement statement = connection.prepareStatement(query.sql())) {
        for (int index = 1; index <= placeholders; index++) {
            statement.setString(index, value);             // Same value for every "?"
        }
        try (ResultSet resultSet = statement.executeQuery()) {
            return readResultSet(resultSet);
        }
    }
}
```

`readResultSet()` uses `ResultSetMetaData` to read column names dynamically, so analytics
query results do not need a pre-declared column list:

```java
private TableData readResultSet(ResultSet resultSet) throws SQLException {
    ResultSetMetaData meta = resultSet.getMetaData();
    int cols = meta.getColumnCount();
    String[] columns = new String[cols];
    for (int i = 0; i < cols; i++) {
        // getColumnLabel returns the AS alias; replace underscores with spaces
        columns[i] = meta.getColumnLabel(i + 1).replace('_', ' ');
    }
    List<Object[]> rows = new ArrayList<>();
    while (resultSet.next()) {
        Object[] row = new Object[cols];
        for (int i = 0; i < cols; i++) {
            row[i] = resultSet.getObject(i + 1);
        }
        rows.add(row);
    }
    return new TableData(columns, rows);
}
```

Column aliases like `Avg_Rating` in the SQL become "Avg Rating" in the table header
(underscore replaced with space).

---

## 9. MongoDB Implementation — MongoCounterStrikeRepository

**File:** `src/main/java/com/counterstrike/app/repository/MongoCounterStrikeRepository.java`

### 9.1 Document Schema Design

The `cs_small` MongoDB database uses an **embedded document** design — related data is
nested inside parent documents rather than stored in separate collections:

```
Collection: people
{
  _id: 1,                         ← PersonID (Long)
  nickname: "s1mple",
  fullName: "Oleksandr Kostyliev",
  birthday: ISODate("2003-05-05"),
  nationality: "Ukraine",
  type: "player",                  ← "player", "coach", or "other"
  playerStats: {                   ← embedded Player table data
    rating: 1.35,
    adr: 82.1,
    dpr: 0.58
  },
  contract: {                      ← embedded PlayerContract data
    teamName: "Natus Vincere",
    startDate: ISODate("2022-01-01"),
    endDate: ISODate("2024-12-31"),
    inGameRole: "Rifler"
  }
}

Collection: matches
{
  _id: 101,
  tournamentId: 5,
  matchDate: ISODate("2024-03-20"),
  teamA: "NaVi",
  teamB: "FaZe",
  winnerTeam: "NaVi",
  records: [                       ← embedded MatchRecord data
    {
      matchRecordId: 201,
      map: "Mirage",
      playerStats: [               ← embedded PlayerMatchStats
        { playerId: 1, kills: 24, deaths: 14, rating: 1.42 },
        ...
      ]
    }
  ]
}
```

This design means a single MongoDB document can contain data that Oracle would split
across 3–4 tables joined by foreign keys.

### 9.2 How Browse Works (Document Scan + Field Extraction)

```java
@Override
public TableData browse(TableSpec spec, String search) {
    String table = spec.tableName();
    List<ColumnSpec> columns = spec.columns();
    String trimmed = (search == null ? "" : search.trim().toLowerCase());

    List<Object[]> rows = new ArrayList<>();
    Iterable<Document> docs = docsFor(table);    // Map table name → MongoDB query

    for (Document doc : docs) {
        Object[] row = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            row[i] = extractField(doc, columns.get(i).name(), table);
        }

        // Text search: keep the row only if any cell contains the search term
        if (!trimmed.isEmpty()) {
            boolean match = false;
            for (Object v : row) {
                if (v != null && v.toString().toLowerCase().contains(trimmed)) {
                    match = true;
                    break;
                }
            }
            if (!match) continue;
        }
        rows.add(row);
    }
    return new TableData(spec.columnLabels(), rows);
}
```

Unlike Oracle's approach (WHERE clause filters at the database), MongoDB's approach
**fetches all documents and filters in Java**. For a small dataset (hundreds of rows)
this is fast enough. For very large datasets it would be inefficient.

### 9.3 How Search Works

The search in MongoDB is a simple Java-side `String.contains()` check:
```java
v.toString().toLowerCase().contains(trimmed)
```
This is equivalent to SQL's `LOWER(col) LIKE '%term%'`. If any extracted field of a
document contains the search term, the row is kept.

### 9.4 Field Mapping: Oracle Names → MongoDB Names

Oracle uses PascalCase column names (`PersonID`, `FullName`); MongoDB uses camelCase field
names (`_id`, `fullName`). `extractField()` handles the translation via a large switch:

```java
private Object extractField(Document doc, String colName, String table) {
    return switch (table + "." + colName) {
        // People table
        case "People.PersonID"    -> doc.get("_id");         // _id stores the Long
        case "People.Nickname"    -> doc.get("nickname");
        case "People.FullName"    -> doc.get("fullName");
        case "People.Birthday"    -> fmt(doc.get("birthday")); // Date → "yyyy-MM-dd"
        case "People.Nationality" -> doc.get("nationality");
        // Player table (embedded in People)
        case "Player.PersonID"    -> doc.get("_id");
        case "Player.Rating"      -> nested(doc, "playerStats", "rating");
        // ...many more cases...
        default -> doc.get(colName);  // Fallback: try the Oracle name as-is
    };
}
```

`nested(doc, parent, child)` safely reads a two-level path:
```java
private static Object nested(Document doc, String parent, String child) {
    Object p = doc.get(parent);
    if (p instanceof Document pd) return pd.get(child);
    return null;   // Parent field missing or not a Document
}
```

### 9.5 Embedded Arrays — expandRecords, expandPlayerStats

Some Oracle tables map to arrays embedded inside MongoDB documents. For example,
`MatchRecord` in Oracle is a separate table; in MongoDB it is the `records[]` array
inside each match document. `docsFor("MatchRecord")` calls `expandRecords()`:

```java
private Iterable<Document> expandRecords() {
    List<Document> result = new ArrayList<>();
    for (Document match : col("matches").find()) {
        Object matchId = match.get("_id");
        List<?> records = (List<?>) match.get("records");
        if (records == null) continue;
        for (Object r : records) {
            if (r instanceof Document rec) {
                // Flatten the array element and inject its parent MatchID
                Document flat = new Document(rec).append("MatchID", matchId);
                result.add(flat);
            }
        }
    }
    return result;
}
```

This **denormalization** converts the nested MongoDB structure back into a flat list of
rows that the generic `browse()` method can process, just as if they were rows in an
Oracle table.

### 9.6 Write Operations — INSERT / UPDATE / DELETE

MongoDB write operations do not use SQL. They use the MongoDB Java Driver's fluent API.

**Inserting a Team:**
```java
case "Team" -> {
    String name = rawValues.get("TeamName");
    requireField(name, "Team Name", 100);
    col("teams").insertOne(
        new Document("_id", name).append("region", rawValues.get("Region"))
    );
}
```
In the teams collection, the `_id` field is the team name (a String), not a numeric ID.
This is valid MongoDB — `_id` can be any type.

**Adding a Player (updating an existing Person document):**
```java
case "Player" -> {
    long id = requireLong(rawValues.get("PersonID"), "Person ID");
    Document stats = new Document("rating", parseDouble(rawValues.get("Rating")))
            .append("adr", parseDouble(rawValues.get("ADR")))
            .append("dpr", parseDouble(rawValues.get("DPR")));

    var res = col("people").updateOne(
        Filters.eq("_id", id),
        Updates.combine(
            Updates.set("type", "player"),
            Updates.set("playerStats", stats)
        )
    );
    if (res.getMatchedCount() == 0) {
        throw new IllegalArgumentException("Person ID " + id + " not found. "
            + "Add the person in the People table first.");
    }
}
```
Because Player data is embedded inside the People document, "adding a player" is actually
an UPDATE: it finds the People document by `_id` and adds/replaces the `playerStats`
sub-document and sets `type = "player"`.

**Deleting a Player (removing embedded fields):**
```java
case "Player" -> {
    long id = parseLongKey(primaryKey.get("PersonID"));
    col("people").updateOne(Filters.eq("_id", id),
        Updates.combine(
            Updates.unset("playerStats"),   // Remove the playerStats sub-document
            Updates.set("type", "other")    // Revert type to generic person
        )
    );
}
```
`Updates.unset("playerStats")` removes that field from the document entirely. The person
still exists in the `people` collection; they just no longer have player stats.

### 9.7 distinctValues — The Codec Problem and Its Fix

The original code called:
```java
col(mongoCol).distinct(mongoField, Object.class)
```

The MongoDB Java Driver 5.x requires a registered **Codec** to deserialize query results.
`Object.class` has no registered Codec, causing this exception at runtime:
```
Can't find codec for CCodecCacheKey(Class java.lang.Object, type = null)
```

The fix is to iterate documents manually and extract the field in Java:

```java
@Override
public List<String> distinctValues(String table, String column) {
    String mongoCol = mongoCollectionFor(table);
    String mongoField = mongoFieldFor(table, column);

    Set<String> seen = new java.util.LinkedHashSet<>();
    for (Document doc : col(mongoCol).find()) {
        Object val = extractNestedValue(doc, mongoField);
        if (val != null) seen.add(fmtKey(val));
    }
    List<String> result = new ArrayList<>(seen);
    Collections.sort(result);
    return result;
}
```

`extractNestedValue()` handles dot-path fields like `"contract.teamName"`:
```java
private static Object extractNestedValue(Document doc, String fieldPath) {
    String[] parts = fieldPath.split("\\.", 2);  // Split at first dot only
    Object val = doc.get(parts[0]);
    if (parts.length == 1 || !(val instanceof Document nested)) return val;
    return extractNestedValue(nested, parts[1]);  // Recurse into the sub-document
}
```

`mongoFieldFor()` maps Oracle column names to their MongoDB equivalents:
```java
private static String mongoFieldFor(String table, String column) {
    return switch (table + "." + column) {
        case "People.PersonID"    -> "_id";
        case "People.Nationality" -> "nationality";   // camelCase
        case "Team.TeamName"      -> "_id";
        case "PlayerContract.TeamName", "CoachContract.TeamName" -> "contract.teamName";
        default -> column;   // Fallback: use Oracle name (may not always match)
    };
}
```

---

## 10. Analytics Layer — AnalyticsQuery, AnalyticsCatalog

### AnalyticsQuery

```java
public record AnalyticsQuery(
    String title,        // Shown in the dropdown, e.g. "Top players by match performance"
    String description,  // Shown below the dropdown as a tooltip-style explanation
    String sql,          // The actual SQL (may contain "?" placeholders)
    String paramLabel    // If non-null, a text field is shown for the user to type a filter
) {
    public boolean hasParameter() { return paramLabel != null && !paramLabel.isBlank(); }

    public int placeholderCount() {
        // Count the number of "?" in the SQL — all get the same value bound to them
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') count++;
        }
        return count;
    }
}
```

**Why `placeholderCount()` instead of a counter field?**
The SQL is written as a text block. Counting `?` characters avoids maintaining a separate
integer that could get out of sync if the SQL is edited.

### AnalyticsCatalog

A static registry of all 8 predefined queries:

| Query | SQL Technique |
|---|---|
| Top players by match performance | `GROUP BY + AVG() + JOIN` |
| Team win/loss record | `CASE WHEN` inside `SUM()`, `NULLIF` for division |
| Active roster of a team | Date comparison with `SYSDATE`, parameterized LIKE |
| Players above average rating | Correlated sub-query `WHERE pl.Rating > (SELECT AVG...)` |
| Tournament participation & placements | Multi-table `JOIN` |
| Map statistics | `GROUP BY Map + COUNT(*) + MAX(RecordDate)` |
| Coach win-rate ranking | Simple `JOIN + ORDER BY` |
| Head-to-head matches | `OR` in WHERE with two placeholders, same value bound twice |

**Oracle-only:** `runAnalytics()` in `MongoCounterStrikeRepository` throws
`UnsupportedOperationException`. The `AnalyticsPanel` detects `!repository.supportsAnalytics()`
and shows an informational notice instead of the query table when in MongoDB mode.

---

## 11. UI Layer

### 11.1 MainFrame — Window Shell and Navigation

**File:** `src/main/java/com/counterstrike/app/ui/MainFrame.java`

```java
public MainFrame(AppRepository repository, String databaseUrl) {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(1000, 660));
    setSize(1200, 780);

    add(header(databaseUrl), BorderLayout.NORTH);   // Navigation + search bar
    add(content(), BorderLayout.CENTER);            // All pages in a CardLayout
    add(footer(), BorderLayout.SOUTH);              // Status bar

    showPage(HOME);
}
```

**`CardLayout` for page switching:**
All seven pages (Home, Players, Teams, Matches, Tournaments, Manage, Analytics) are
constructed once at startup and placed in a `CardLayout` container. Switching pages is
`cardLayout.show(cards, pageName)` — it does not re-create any component. This makes
navigation instant and avoids repeated construction costs.

**Active navigation button highlighting:**
```java
private void showPage(String page) {
    currentPage = page;
    cardLayout.show(cards, page);

    navButtons.forEach((name, button) -> {
        if (name.equals(page)) {
            button.setBackground(ACCENT);      // Gold highlight for active page
            button.setForeground(Color.BLACK);
        } else {
            button.setBackground(null);        // FlatLaf default for inactive
            button.setForeground(null);
        }
    });

    reloadCurrentPage();
}
```

**`reloadCurrentPage()`** dispatches to the appropriate load method based on which
page is currently visible:
```java
private void reloadCurrentPage() {
    switch (currentPage) {
        case HOME       -> loadHome();
        case PLAYERS    -> loadTable(playersTable, "Players", repository::findTopPlayers);
        case TEAMS      -> loadTable(teamsTable,   "Teams",   repository::findTrendingTeams);
        case MATCHES    -> loadTable(matchesTable,  "Matches", repository::findTeamMatchHistory);
        case TOURNAMENTS-> loadTable(tournamentsTable, "Tournaments",
                                     repository::findTournamentMatchSummary);
        case MANAGE, ANALYTICS -> setStatus(currentPage + " | use the controls on this page");
    }
}
```

The Manage and Analytics pages manage their own refresh cycles internally, so
`reloadCurrentPage()` does nothing for them except update the status bar.

**Color palette (CS2-inspired):**
```java
static final Color ACCENT       = new Color(0xDE9B35); // Gold/amber — active nav, metric numbers
static final Color ACCENT_DIM   = new Color(0x8B6914); // Darker gold — buttons
static final Color BG_DARKER    = new Color(0x1A1A24); // Darkest background
static final Color BG_CARD      = new Color(0x242436); // Card and table background
static final Color TEXT_PRIMARY = new Color(0xE8E8E8); // Main text
static final Color TEXT_MUTED   = new Color(0x8E8E9A); // Labels, hints
static final Color BORDER_SUBTLE= new Color(0x363650); // Card borders
static final Color TABLE_ALT    = new Color(0x1E1E2E); // Alternating table row
```

### 11.2 SwingWorker — Background Threading Model

Swing uses a single **Event Dispatch Thread (EDT)** for all UI operations. If you
perform a database query on the EDT, the entire window freezes until the query returns —
buttons stop responding, the cursor spins, and the window may appear unresponsive.

**Rule: all database calls must run on a background thread; all UI updates must run on the EDT.**

`SwingWorker<T, V>` enforces this pattern:

```java
private void loadTable(JTable table, String page, TableLoader loader) {
    String search = searchField.getText();
    setStatus("Loading " + page + "...");

    new SwingWorker<TableData, Void>() {

        @Override
        protected TableData doInBackground() throws Exception {
            // Runs on a background thread from SwingWorker's thread pool
            return loader.load(search);   // Calls the repository — JDBC / MongoDB I/O here
        }

        @Override
        protected void done() {
            // Automatically scheduled back on the EDT when doInBackground() finishes
            try {
                TableData data = get();           // get() re-throws any exception from background
                setTableData(table, data);        // Safe — we're on the EDT now
                setStatus(page + " | rows: " + data.rowCount());
            } catch (Exception exception) {
                showQueryError(exception);        // Show error dialog on EDT
            }
        }

    }.execute();   // Submits doInBackground() to the worker thread pool
}
```

`loader` is a `@FunctionalInterface`:
```java
@FunctionalInterface
private interface TableLoader {
    TableData load(String search) throws Exception;
}
```

This allows passing `repository::findTopPlayers` as a method reference — the actual
method is called inside `doInBackground()` on the background thread.

### 11.3 ReadOnlyTableModel — Feeding Data to JTable

**File:** `src/main/java/com/counterstrike/app/ui/ReadOnlyTableModel.java`

Swing's `JTable` requires a `TableModel` to supply its data. `ReadOnlyTableModel`
extends `AbstractTableModel` and wraps a `TableData`:

```java
public final class ReadOnlyTableModel extends AbstractTableModel {
    private String[] columns = new String[0];
    private Object[][] rows   = new Object[0][0];

    public void setData(TableData data) {
        columns = data.columns();
        rows    = data.rows().toArray(Object[][]::new);
        fireTableStructureChanged();  // Notifies JTable to re-render everything
    }

    @Override public int    getRowCount()                    { return rows.length; }
    @Override public int    getColumnCount()                 { return columns.length; }
    @Override public String getColumnName(int col)           { return columns[col]; }
    @Override public Object getValueAt(int row, int col)     { return rows[row][col]; }
    @Override public boolean isCellEditable(int row, int col){ return false; } // Read-only
}
```

`isCellEditable` returns `false` for all cells. This prevents users from directly typing
into the table — all edits must go through the Add/Edit form dialog.

`fireTableStructureChanged()` tells Swing the table's column count may have changed
(not just the data). This is necessary when switching between tables with different
column counts (e.g., from People with 5 columns to Player with 4 columns).

**`TableRowSorter` (sort + filter):**
When `JTable.setAutoCreateRowSorter(true)` is called, Swing automatically creates a
`TableRowSorter<ReadOnlyTableModel>`. Clicking a column header sorts rows without
touching the underlying model. The `CrudPanel` column filter installs a `RowFilter` on
this same sorter.

### 11.4 Styles — Shared Widget Factory

**File:** `src/main/java/com/counterstrike/app/ui/Styles.java`

```java
final class Styles {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static JTable styledTable() { ... }       // Creates a JTable with dark theme + alternating rows
    static JButton accentButton(String text) { ... }  // Gold-tinted button
    static String asEditText(Object value) { ... }    // Formats a cell value for a text field
}
```

**`asEditText(Object value)`** is used when pre-filling an edit form with existing data:
```java
static String asEditText(Object value) {
    if (value == null) return "";
    if (value instanceof Date date) return DATE_FORMAT.format(date);  // java.sql.Date → "2003-05-15"
    return String.valueOf(value);
}
```

Oracle's JDBC driver returns `DATE` column values as `java.sql.Date` objects. Without
this conversion, the edit form would show something like "Mon May 05 00:00:00 UTC 2003"
instead of "2003-05-05".

**Alternating row colors** in `styledTable()`:
```java
table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(JTable tbl, Object value,
            boolean isSelected, boolean hasFocus, int row, int col) {
        // Format Date objects as strings before rendering
        Object display = (value instanceof Date date) ? DATE_FORMAT.format(date) : value;
        Component c = super.getTableCellRendererComponent(tbl, display, ...);
        if (!isSelected) {
            c.setBackground(row % 2 == 0 ? BG_CARD : TABLE_ALT);  // Stripe effect
        }
        return c;
    }
});
```

Every even row gets `BG_CARD` (#242436) and every odd row gets `TABLE_ALT` (#1E1E2E),
creating a subtle banding effect that makes rows easier to read.

### 11.5 CrudPanel — The Manage Page

**File:** `src/main/java/com/counterstrike/app/ui/CrudPanel.java`

This is the most complex UI component. It provides full CRUD for any table in `Schema.ALL`.

#### Table selection and loading

```java
tableSelect.addActionListener(e -> {
    currentSpec = (TableSpec) tableSelect.getSelectedItem();
    currentColumns = currentSpec.columns();   // Used by filter
    clearFilter();                            // Reset any active filter
    reload();                                 // Fetch data for the new table
});
```

`reload()` calls `repository.browse(currentSpec, searchField.getText())` on a
`SwingWorker` background thread, then calls `setData()` on the `ReadOnlyTableModel`.

#### Column filter — updateFilterOperators()

```java
private void updateFilterOperators() {
    int idx = filterCol.getSelectedIndex();
    ColumnSpec.Type type = (idx >= 0 && idx < currentColumns.size())
            ? currentColumns.get(idx).type() : ColumnSpec.Type.TEXT;

    String[] ops = switch (type) {
        case TEXT    -> new String[]{"contains", "=", "≠"};
        case DATE    -> new String[]{"=", "≠", ">=", "<=", ">", "<"};
        default      -> new String[]{">", "<", ">=", "<=", "=", "≠"};  // INTEGER, DECIMAL
    };

    filterOp.removeAllItems();
    for (String op : ops) filterOp.addItem(op);
}
```

When the user changes the selected filter column, this method replaces the operator
dropdown's items. Text columns only get `contains / = / ≠` because `>` / `<` on arbitrary
strings is confusing. Date and number columns get all comparison operators.

#### Column filter — applyFilter()

```java
private void applyFilter() {
    int colIdx = filterCol.getSelectedIndex();
    String op  = (String) filterOp.getSelectedItem();
    String val = filterVal.getText().trim();

    // ── Input validation ──────────────────────────────────────────────────
    ColumnSpec.Type colType = currentColumns.get(colIdx).type();

    if (colType == INTEGER || colType == DECIMAL) {
        try { Double.parseDouble(val); }
        catch (NumberFormatException e) {
            statusLabel.setText("Invalid filter: \"" + val + "\" is not a number.");
            return;  // Do NOT apply the filter
        }
    }
    if (colType == DATE) {
        if (!val.matches("\\d{4}-\\d{2}-\\d{2}")) {
            statusLabel.setText("Invalid filter: date must be YYYY-MM-DD.");
            return;
        }
    }

    // ── Install the RowFilter ─────────────────────────────────────────────
    var sorter = (TableRowSorter<ReadOnlyTableModel>) table.getRowSorter();
    sorter.setRowFilter(new RowFilter<>() {
        @Override
        public boolean include(Entry entry) {
            Object cell = entry.getValue(colIdx);
            if (cell == null) return false;
            String cellStr = cell.toString();

            if (colType == DATE) {
                // Oracle Timestamp: "1990-05-15 00:00:00.0" → trim to "1990-05-15"
                String cellDate = cellStr.length() > 10 && cellStr.charAt(10) == ' '
                        ? cellStr.substring(0, 10) : cellStr;
                int cmp = cellDate.compareTo(val);
                return switch (op) {
                    case ">"  -> cmp >  0;
                    case "<"  -> cmp <  0;
                    case ">=" -> cmp >= 0;
                    case "<=" -> cmp <= 0;
                    case "="  -> cmp == 0;
                    case "≠"  -> cmp != 0;
                    default   -> cellDate.contains(val);
                };
            }

            if (colType == INTEGER || colType == DECIMAL) {
                double cellNum = Double.parseDouble(cellStr);
                double filterNum = Double.parseDouble(val);
                return switch (op) {
                    case ">"  -> cellNum >  filterNum;
                    case "<"  -> cellNum <  filterNum;
                    case ">=" -> cellNum >= filterNum;
                    case "<=" -> cellNum <= filterNum;
                    case "="  -> cellNum == filterNum;
                    case "≠"  -> cellNum != filterNum;
                    default   -> cellStr.toLowerCase().contains(val.toLowerCase());
                };
            }

            // TEXT
            return switch (op) {
                case "="        -> cellStr.equalsIgnoreCase(val);
                case "≠"        -> !cellStr.equalsIgnoreCase(val);
                default         -> cellStr.toLowerCase().contains(val.toLowerCase());
            };
        }
    });
}
```

**Key insight — `RowFilter` is client-side:** The filter does not issue a new database
query. All rows are already in memory inside `ReadOnlyTableModel`. `RowFilter.include()`
is called for each row by Swing's `TableRowSorter`, and rows that return `false` are
hidden from view (but not removed from the model). Clearing the filter with `clearFilter()`
calls `sorter.setRowFilter(null)`, making all rows visible again instantly.

**Why date comparison uses `String.compareTo()` not `Date` parsing:**
Dates stored as `"yyyy-MM-dd"` strings are lexicographically sortable — `"2024-01-15".compareTo("2024-01-01")` correctly returns positive. Parsing them into `LocalDate` objects would work too, but the string comparison is simpler and equally correct for this format.

#### openForm() — Dynamically generating Add/Edit forms

```java
private void openForm(boolean edit) {
    // Step 1: Background thread — fetch dropdown options for all FK columns
    new SwingWorker<Map<String, List<String>>, Void>() {
        @Override
        protected Map<String, List<String>> doInBackground() throws Exception {
            Map<String, List<String>> options = new LinkedHashMap<>();
            for (ColumnSpec column : spec.columns()) {
                if (column.foreignKey() != null) {
                    options.put(column.name(),
                        repository.distinctValues(
                            column.foreignKey().table(),
                            column.foreignKey().column()));
                }
            }
            return options;
        }
        @Override
        protected void done() {
            showForm(spec, edit, existing, primaryKey, get());
        }
    }.execute();
}
```

```java
private void showForm(TableSpec spec, boolean edit, Map<String, String> existing,
                      Map<String, String> primaryKey, Map<String, List<String>> fkOptions) {
    JPanel form = new JPanel(new GridBagLayout());
    Map<String, JComponent> fields = new LinkedHashMap<>();

    for (ColumnSpec column : spec.columns()) {
        // Add label: "Rating" or "Birthday (YYYY-MM-DD)" or "Player ID *"
        form.add(new JLabel(column.label()
                + (column.required() ? " *" : "")
                + (column.isDate() ? " (YYYY-MM-DD)" : "")), gc);

        JComponent field;
        boolean locked = edit && column.primaryKey();  // PK cannot be changed when editing

        if (column.foreignKey() != null && !locked) {
            // FK column → editable dropdown with existing values as options
            JComboBox<String> combo = new JComboBox<>();
            combo.setEditable(true);
            if (!column.required()) combo.addItem("");  // Allow blank for optional FKs
            for (String opt : fkOptions.getOrDefault(column.name(), List.of())) {
                combo.addItem(opt);
            }
            combo.setSelectedItem(existing.getOrDefault(column.name(), ""));
            field = combo;
        } else {
            // Regular column → text field
            JTextField text = new JTextField(existing.getOrDefault(column.name(), ""), 22);
            text.setEnabled(!locked);  // Grayed out for PK in edit mode
            field = text;
        }
        form.add(field, gc);
        fields.put(column.name(), field);
    }

    int choice = JOptionPane.showConfirmDialog(this, form, (edit ? "Edit " : "Add ") + spec.displayName(), ...);
    if (choice != JOptionPane.OK_OPTION) return;

    // Collect values and save
    Map<String, String> values = new LinkedHashMap<>();
    for (ColumnSpec column : spec.columns()) {
        values.put(column.name(), readComponent(fields.get(column.name())));
    }
    save(spec, edit, values, primaryKey);
}
```

The form is built entirely from `TableSpec.columns()`. Adding a column to `Schema.java`
automatically adds it to this form with the appropriate widget type.

### 11.6 AnalyticsPanel — The Analytics Page

```java
AnalyticsPanel(AppRepository repository) {
    if (!repository.supportsAnalytics()) {
        // MongoDB mode: show a notice instead of the query UI
        add(new JLabel("Advanced SQL queries require an Oracle connection."),
            BorderLayout.CENTER);
    } else {
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    for (AnalyticsQuery query : AnalyticsCatalog.QUERIES) {
        querySelect.addItem(query);  // AnalyticsQuery.toString() returns its title
    }
    querySelect.addActionListener(e -> onQuerySelected());
}
```

**`onQuerySelected()`** updates the UI to reflect whether the chosen query has a
parameter:
```java
private void onQuerySelected() {
    AnalyticsQuery query = (AnalyticsQuery) querySelect.getSelectedItem();
    descriptionLabel.setText(query.description());
    boolean hasParam = query.hasParameter();
    paramLabel.setVisible(hasParam);   // Show/hide "Team name contains:" label
    paramField.setVisible(hasParam);   // Show/hide the text input
    paramField.setText("");
}
```

**`run()`** executes the selected query on a background thread:
```java
private void run() {
    AnalyticsQuery query = (AnalyticsQuery) querySelect.getSelectedItem();
    String parameter = query.hasParameter() ? paramField.getText() : "";

    new SwingWorker<TableData, Void>() {
        @Override
        protected TableData doInBackground() throws Exception {
            return repository.runAnalytics(query, parameter);
        }
        @Override
        protected void done() {
            TableData data = get();
            ((ReadOnlyTableModel) table.getModel()).setData(data);
        }
    }.execute();
}
```

---

## 12. End-to-End Data Flows

### 12.1 Searching for a Player

```
User types "s1mple" in the search bar and presses Enter
  ↓
searchField ActionListener fires on the EDT
  ↓
MainFrame.reloadCurrentPage() → loadTable(playersTable, "Players", repository::findTopPlayers)
  ↓
SwingWorker.execute() — submit doInBackground to worker thread pool
  ↓
[Background Thread]
  CounterStrikeRepository.findTopPlayers("s1mple")
  → like("s1mple") = "%s1mple%"
  → SQL: SELECT p.PersonID, p.Nickname, p.FullName, pl.Rating, pl.ADR, pl.DPR
          FROM People p JOIN Player pl ON p.PersonID = pl.PersonID
          WHERE LOWER(p.FullName) LIKE '%s1mple%' OR ...
  → JDBC sends query to Oracle via TCP
  → Oracle executes query, returns ResultSet
  → queryTable() reads ResultSet into List<Object[]>
  → Returns TableData(["PlayerID","Nickname","Name","Rating","ADR","DPR"], rows)
  ↓
[EDT — SwingWorker.done()]
  ReadOnlyTableModel.setData(tableData)
  → fireTableStructureChanged()
  → JTable re-renders with new data
  statusLabel: "Players | rows: 3"
```

### 12.2 Adding a New Row

```
User selects "Player" table, clicks "Add"
  ↓
CrudPanel.openForm(false)
  ↓
SwingWorker.doInBackground():
  Player table has column PersonID with FK → People.PersonID
  → repository.distinctValues("People", "PersonID")
  → SQL: SELECT DISTINCT PersonID FROM People WHERE PersonID IS NOT NULL ORDER BY PersonID
  → Returns ["1", "2", "7", "12", ...]
  ↓
SwingWorker.done():
  showForm(PLAYER_SPEC, edit=false, existing={}, {}, {PersonID: ["1","2","7"...]})
  → Builds form: PersonID=dropdown(["1","2"...]), Rating=textfield, ADR=textfield, DPR=textfield
  ↓
User selects PersonID=7, types Rating=1.15, ADR=78.3, DPR=0.61, clicks OK
  ↓
CrudPanel.save(PLAYER_SPEC, edit=false, values={PersonID:"7", Rating:"1.15", ...}, {})
  ↓
SwingWorker.doInBackground():
  repository.insertRow(PLAYER_SPEC, values)
  → columns = [PersonID, Rating, ADR, DPR]
  → SQL: INSERT INTO Player (PersonID, Rating, ADR, DPR) VALUES (?, ?, ?, ?)
  → bind(1, INTEGER, "7")   → statement.setLong(1, 7L)
  → bind(2, DECIMAL, "1.15") → statement.setBigDecimal(2, BigDecimal(1.15))
  → bind(3, DECIMAL, "78.3") → statement.setBigDecimal(3, ...)
  → bind(4, DECIMAL, "0.61") → ...
  → executeWrite(): setAutoCommit(false) → executeUpdate() → commit()
  ↓
SwingWorker.done():
  statusLabel: "Row added."
  reload() → re-fetches the Player table to show the new row
```

### 12.3 Editing an Existing Row

```
User selects row (PersonID=7, Rating=1.15), clicks "Edit"
  ↓
CrudPanel reads selected row from table model:
  existing = {PersonID:"7", Rating:"1.15", ADR:"78.3", DPR:"0.61"}
  primaryKey = {PersonID:"7"}
  ↓
openForm(edit=true) → showForm()
  → PersonID field: text field, disabled (locked, cannot be changed)
  → Rating, ADR, DPR: text fields pre-filled with current values
  ↓
User changes Rating to 1.20, clicks OK
  ↓
values = {PersonID:"7", Rating:"1.20", ADR:"78.3", DPR:"0.61"}
  ↓
repository.updateRow(PLAYER_SPEC, primaryKey={PersonID:"7"}, values)
  → editable = [Rating, ADR, DPR]  (PersonID excluded — it's the PK)
  → SQL: UPDATE Player SET Rating = ?, ADR = ?, DPR = ? WHERE PersonID = ?
  → Bind SET: Rating=1.20, ADR=78.3, DPR=0.61
  → Bind WHERE: PersonID=7
  → executeWrite() → commit()
  → affected=1 → success
```

### 12.4 Deleting a Row

```
User selects row (PersonID=7), clicks "Delete"
  ↓
CrudPanel reads PK from selected row:
  primaryKey = {PersonID:"7"}
  ↓
Confirmation dialog: "Delete this Player row? Person ID = 7 — This cannot be undone."
  ↓
User clicks Yes
  ↓
repository.deleteRow(PLAYER_SPEC, {PersonID:"7"})
  → SQL: DELETE FROM Player WHERE PersonID = ?
  → Bind: PersonID=7
  → executeWrite() → commit()
  → affected=1 → success
  ↓
reload() — table refreshes, row is gone
```

### 12.5 Column Filter (Client-Side)

```
Manage page shows People table (200 rows already loaded in memory)
User selects column "Birthday", operator "=", types "1990-05-15", clicks Apply
  ↓
applyFilter()
  colType = DATE
  Validation: "1990-05-15".matches("\\d{4}-\\d{2}-\\d{2}") → passes
  ↓
Install RowFilter on TableRowSorter:
  For each row in the model:
    cell = row[3]   (Birthday column, index 3)
    cellStr = "1990-05-15 00:00:00.0"  (Oracle Timestamp.toString())
    cellStr.charAt(10) == ' ' → true
    cellDate = "1990-05-15"             (first 10 chars)
    op = "=" → cellDate.compareTo("1990-05-15") == 0 → true → include this row
    
    Other rows: cellDate ≠ "1990-05-15" → excluded
  ↓
JTable hides all non-matching rows
No database query is made — all data already in memory
Status bar: "Filter: Birthday = 1990-05-15  →  1 rows"
```

### 12.6 Switching Databases at Runtime

```
User clicks "Switch DB" button in the search bar
  ↓
CounterStrikeApp.connect(MainFrame.this) called on EDT
  ↓
JOptionPane shows "Oracle / MongoDB" dialog
User selects MongoDB, provides URI
  ↓
MongoDatabase mongo = new MongoDatabase("mongodb://root:example@localhost:27017", "cs_small")
mongo.testConnection() → ping succeeds
repository = new MongoCounterStrikeRepository(mongo)
  ↓
MainFrame newFrame = new MainFrame(repository, uri + "/cs2")
newFrame.setVisible(true)
oldFrame.dispose()     ← The old Oracle window is closed
  ↓
New window appears with MongoDB data
```

---

## 13. Design Principles Summary

| Principle | Where Applied | Benefit |
|---|---|---|
| **Interface Isolation** | `AppRepository` interface | UI never imports Oracle or MongoDB classes |
| **Metadata-Driven Code** | `Schema`, `TableSpec`, `ColumnSpec` | Adding a table needs zero SQL / zero UI code |
| **PreparedStatement** | All JDBC queries | SQL injection impossible for user input |
| **Explicit Transactions** | `executeWrite()` | INSERT/UPDATE/DELETE roll back automatically on error |
| **Background Threading** | All `SwingWorker` uses | UI never freezes during database I/O |
| **Fail-Fast** | `testConnection()` at startup | Error shown before UI is built |
| **Immutable Records** | `TableData`, `TableSpec`, `HomeSummary` | No accidental mutation of shared data |
| **Client-Side Filtering** | `RowFilter` in `CrudPanel` | Column filter is instant — no extra DB round-trip |
| **Lazy DB Connection** | `MongoDatabase.getDatabase()` | MongoDB connection pool not created until first use |
| **Type-Safe Binding** | `bind()` in repository | Dates and numbers are correctly typed in SQL |
