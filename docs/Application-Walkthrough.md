# CS2 Database Browser — Application Walkthrough

## Overview

CS2 Database Browser is a desktop application for browsing, managing, and analysing
Counter-Strike 2 player and tournament data. It supports two interchangeable database
backends: **Oracle** (relational) and **MongoDB** (NoSQL), and can switch between them
at runtime without restarting.

---

## Starting the Application

1. Start **Docker Desktop** and run the database containers:
   ```
   docker start cs2-oracle cs_mongo_small
   ```
2. Launch the JAR:
   ```
   java -jar target/counterstrike-browser-1.0.0-shaded.jar
   ```
3. A connection dialog appears. Choose **Oracle** or **MongoDB** and confirm.

---

## Navigation

The top bar contains seven pages:

| Page | What it shows |
|---|---|
| **Home** | Summary counts (players, teams, matches, tournaments) + top player + trending team |
| **Players** | All players ranked by rating |
| **Teams** | All teams with recent match count and wins |
| **Matches** | Full match history with scores, tournament and winner |
| **Tournaments** | Tournament list with start/end dates and match count |
| **Manage** | Full CRUD (add / edit / delete) for every table |
| **Analytics** | Eight pre-built advanced queries (Oracle only) |

The **Search** bar at the top filters the current page across all columns simultaneously.
The **Refresh** button reloads data from the database.
The **Switch DB** button (next to Refresh) reopens the connection dialog to switch backends at runtime.

---

## Manage Page

Select a table from the dropdown on the left. The table loads automatically.

### Adding a row
Click **Add** → fill in the form → click OK.
- Fields marked **\*** are required.
- Date fields require **YYYY-MM-DD** format (e.g. `2003-05-15`).
- Foreign-key fields show a dropdown of existing values; you can also type freely.

### Editing a row
Select a row in the table → click **Edit** → modify the non-key fields → click OK.
Primary key fields are greyed out and cannot be changed.

### Deleting a row
Select a row → click **Delete** → confirm the dialog.
This action cannot be undone.

### Column Filter
Below the table selector is a column-level filter:

1. Select the **column** to filter on.
2. Choose an **operator** — operators adapt to the column type:
   - **Text columns**: `contains`, `=`, `≠`
   - **Date columns**: `=`, `≠`, `>=`, `<=`, `>`, `<` (format: `YYYY-MM-DD`)
   - **Number columns**: `>`, `<`, `>=`, `<=`, `=`, `≠`
3. Type a **value** and click **Apply**.
4. Click **Clear** to remove the filter.

The filter works on data already loaded in memory — no extra database query is made.
Typing a letter in a number column, or a wrong date format, shows an error in the status
bar and does not apply the filter.

---

## Analytics Page (Oracle only)

Select a query from the dropdown. Some queries show an optional text input for filtering
(e.g. filtering by team name). Click **Run** to execute.

Available queries:

| Query | Description |
|---|---|
| Top players by match performance | Average in-match rating, kills, ADR across all maps played |
| Team win/loss record | Total matches, wins, losses and win percentage per team |
| Active roster of a team | Current players via active contracts |
| Players above average rating | Players whose rating exceeds the overall average |
| Tournament participation & placements | Every team's placement per tournament |
| Map statistics | How often each map was played |
| Coach win-rate ranking | Coaches ranked by win rate |
| Head-to-head matches | All matches involving a specific team |

> Analytics queries are not available in MongoDB mode. Switch to Oracle to use this page.

---

## Switching Databases

Click **Switch DB** in the search bar at any time. The connection dialog reappears.
Selecting a new backend opens a fresh window with the new data; the old window closes automatically.

---

## Data Model Summary

| Table | Key fields |
|---|---|
| People | PersonID, Nickname, Full Name, Birthday, Nationality |
| Player | PersonID (→ People), Rating, ADR, DPR |
| Coach | PersonID (→ People), Win Rate |
| Team | Team Name, Region |
| Player Contract | Person, Team, Start/End Date, In-Game Role |
| Coach Contract | Person, Team, Start/End Date |
| Tournament | Name, Start/End Date |
| Match | Teams, Date, Stage, Score, Winner |
| Match Record | Match, Map, Scores per half |
| Player Match Stats | Player, Map, Kills, Deaths, Assists, Rating, ADR, DPR |
| Tournament Participation | Tournament, Team, Placement |
