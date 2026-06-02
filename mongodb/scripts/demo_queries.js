db = db.getSiblingDB("cs_small");

print("== Collection counts ==");
printjson({
  teams: db.teams.countDocuments(),
  people: db.people.countDocuments(),
  tournaments: db.tournaments.countDocuments(),
  matches: db.matches.countDocuments()
});

print("\n== Point query: tournament 1 Grand Final ==");
printjson(
  db.matches
    .find(
      { tournamentId: 1, stage: "Grand Final" },
      { _id: 1, matchDate: 1, stage: 1, teamA: 1, teamB: 1, winnerTeam: 1, records: 1 }
    )
    .toArray()
);

print("\n== Range query: matches from 2025-12-12 to 2025-12-15 ==");
printjson(
  db.matches
    .find(
      {
        matchDate: {
          $gte: ISODate("2025-12-12"),
          $lte: ISODate("2025-12-15")
        }
      },
      { _id: 1, matchDate: 1, stage: 1, teamA: 1, teamB: 1, winnerTeam: 1 }
    )
    .sort({ matchDate: 1, _id: 1 })
    .toArray()
);

print("\n== Player stats lookup: donk across embedded match records ==");
printjson(
  db.matches
    .aggregate([
      { $unwind: "$records" },
      { $unwind: "$records.playerStats" },
      { $match: { "records.playerStats.playerId": 11 } },
      {
        $project: {
          _id: 0,
          matchId: "$_id",
          stage: 1,
          matchDate: 1,
          map: "$records.map",
          team: "$records.playerStats.teamName",
          kills: "$records.playerStats.kills",
          deaths: "$records.playerStats.deaths",
          rating: "$records.playerStats.rating"
        }
      },
      { $sort: { matchDate: 1, matchId: 1 } }
    ])
    .toArray()
);

print("\n== Explain: point query uses compound index ==");
printjson(
  db.matches
    .find({ tournamentId: 1, stage: "Grand Final" })
    .explain("executionStats").queryPlanner.winningPlan
);

print("\n== Explain: range query uses date index ==");
printjson(
  db.matches
    .find({
      matchDate: {
        $gte: ISODate("2025-12-12"),
        $lte: ISODate("2025-12-15")
      }
    })
    .explain("executionStats").queryPlanner.winningPlan
);
