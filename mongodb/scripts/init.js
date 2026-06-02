const dbName = "cs_small";
db = db.getSiblingDB(dbName);

db.dropDatabase();

db.teams.insertMany([
  { _id: "The MongolZ", region: "Asia" },
  { _id: "MOUZ", region: "Europe" },
  { _id: "Team Spirit", region: "CIS" },
  { _id: "Team Liquid", region: "Americas" },
  { _id: "G2 Esports", region: "Europe" },
  { _id: "HEROIC", region: "Europe" },
  { _id: "Team Vitality", region: "Europe" },
  { _id: "FaZe Clan", region: "Europe" }
]);

db.people.insertMany([
  {
    _id: 1,
    nickname: "mzinho",
    fullName: "Ayush Batbold",
    birthday: ISODate("2007-05-31"),
    nationality: "Mongolia",
    type: "player",
    playerStats: { rating: 1.01, adr: 71.2, dpr: 0.69 },
    contract: { teamName: "The MongolZ", startDate: ISODate("2023-03-01"), endDate: null, inGameRole: "Rifler" }
  },
  {
    _id: 2,
    nickname: "Techno",
    fullName: "Munkhbold Sodbayar",
    birthday: ISODate("2005-05-18"),
    nationality: "Mongolia",
    type: "player",
    playerStats: { rating: 1.04, adr: 76.5, dpr: 0.72 },
    contract: { teamName: "The MongolZ", startDate: ISODate("2023-03-01"), endDate: null, inGameRole: "Rifler" }
  },
  {
    _id: 6,
    nickname: "Brollan",
    fullName: "Ludvig Brolin",
    birthday: ISODate("2002-06-17"),
    nationality: "Sweden",
    type: "player",
    playerStats: { rating: 1.11, adr: 80.5, dpr: 0.69 },
    contract: { teamName: "MOUZ", startDate: ISODate("2023-12-01"), endDate: null, inGameRole: "Rifler" }
  },
  {
    _id: 11,
    nickname: "donk",
    fullName: "Danil Kryshkovets",
    birthday: ISODate("2007-01-25"),
    nationality: "Russia",
    type: "player",
    playerStats: { rating: 1.32, adr: 90.4, dpr: 0.66 },
    contract: { teamName: "Team Spirit", startDate: ISODate("2023-07-01"), endDate: null, inGameRole: "Rifler" }
  },
  {
    _id: 12,
    nickname: "sh1ro",
    fullName: "Dmitry Sokolov",
    birthday: ISODate("2001-07-15"),
    nationality: "Russia",
    type: "player",
    playerStats: { rating: 1.21, adr: 76.6, dpr: 0.59 },
    contract: { teamName: "Team Spirit", startDate: ISODate("2023-12-01"), endDate: null, inGameRole: "AWPer" }
  },
  {
    _id: 16,
    nickname: "Twistzz",
    fullName: "Russel Van Dulken",
    birthday: ISODate("1999-11-14"),
    nationality: "Canada",
    type: "player",
    playerStats: { rating: 1.14, adr: 79.5, dpr: 0.65 },
    contract: { teamName: "Team Liquid", startDate: ISODate("2023-12-01"), endDate: null, inGameRole: "IGL" }
  },
  {
    _id: 21,
    nickname: "NiKo",
    fullName: "Nikola Kovac",
    birthday: ISODate("1997-02-16"),
    nationality: "Bosnia and Herzegovina",
    type: "player",
    playerStats: { rating: 1.20, adr: 84.4, dpr: 0.70 },
    contract: { teamName: "G2 Esports", startDate: ISODate("2020-11-01"), endDate: null, inGameRole: "Rifler" }
  },
  {
    _id: 31,
    nickname: "ZywOo",
    fullName: "Mathieu Herbaut",
    birthday: ISODate("2000-11-09"),
    nationality: "France",
    type: "player",
    playerStats: { rating: 1.31, adr: 85.5, dpr: 0.61 },
    contract: { teamName: "Team Vitality", startDate: ISODate("2018-10-01"), endDate: null, inGameRole: "AWPer" }
  },
  {
    _id: 36,
    nickname: "ropz",
    fullName: "Robin Kool",
    birthday: ISODate("1999-12-22"),
    nationality: "Estonia",
    type: "player",
    playerStats: { rating: 1.16, adr: 78.5, dpr: 0.61 },
    contract: { teamName: "FaZe Clan", startDate: ISODate("2022-01-01"), endDate: null, inGameRole: "Lurker" }
  },
  {
    _id: 48,
    nickname: "NEO",
    fullName: "Filip Kubski",
    birthday: ISODate("1987-06-15"),
    nationality: "Poland",
    type: "coach",
    coachStats: { winRate: 57.9 },
    contract: { teamName: "FaZe Clan", startDate: ISODate("2023-07-27"), endDate: null }
  }
]);

db.tournaments.insertOne({
  _id: 1,
  tournamentName: "Perfect World Shanghai Major 2025",
  startDate: ISODate("2025-11-30"),
  endDate: ISODate("2025-12-15"),
  participation: [
    { teamName: "Team Spirit", placement: "1st" },
    { teamName: "FaZe Clan", placement: "2nd" },
    { teamName: "G2 Esports", placement: "3-4th" },
    { teamName: "MOUZ", placement: "3-4th" },
    { teamName: "Team Vitality", placement: "5-8th" },
    { teamName: "HEROIC", placement: "5-8th" },
    { teamName: "Team Liquid", placement: "5-8th" },
    { teamName: "The MongolZ", placement: "5-8th" }
  ]
});

db.matches.insertMany([
  {
    _id: 1,
    tournamentId: 1,
    matchDate: ISODate("2025-12-11"),
    stage: "Quarter-finals",
    teamA: "The MongolZ",
    teamB: "MOUZ",
    matchResultTeamA: "0",
    matchResultTeamB: "2",
    winnerTeam: "MOUZ",
    winningCondition: "Best of 3",
    records: [
      {
        matchRecordId: 1,
        recordDate: ISODate("2025-12-11"),
        startingSide: "CT",
        finalScore: "14-16",
        topHalfScore: "7-5",
        bottomHalfScore: "6-7",
        teamAResult: "Loss",
        teamBResult: "Win",
        map: "Ancient",
        playerStats: [
          { playerId: 1, nickname: "mzinho", teamName: "The MongolZ", kills: 28, deaths: 22, assists: 7, rating: 1.44, adr: 100.5, dpr: 0.73 },
          { playerId: 2, nickname: "Techno", teamName: "The MongolZ", kills: 28, deaths: 20, assists: 3, rating: 1.38, adr: 84.7, dpr: 0.67 },
          { playerId: 6, nickname: "Brollan", teamName: "MOUZ", kills: 26, deaths: 20, assists: 9, rating: 1.35, adr: 101.1, dpr: 0.67 }
        ]
      },
      {
        matchRecordId: 2,
        recordDate: ISODate("2025-12-11"),
        startingSide: "T",
        finalScore: "9-13",
        topHalfScore: "4-8",
        bottomHalfScore: "5-5",
        teamAResult: "Loss",
        teamBResult: "Win",
        map: "Nuke",
        playerStats: [
          { playerId: 1, nickname: "mzinho", teamName: "The MongolZ", kills: 15, deaths: 18, assists: 4, rating: 0.92, adr: 67.4, dpr: 0.78 },
          { playerId: 6, nickname: "Brollan", teamName: "MOUZ", kills: 20, deaths: 13, assists: 6, rating: 1.28, adr: 88.9, dpr: 0.59 }
        ]
      }
    ]
  },
  {
    _id: 2,
    tournamentId: 1,
    matchDate: ISODate("2025-12-11"),
    stage: "Quarter-finals",
    teamA: "Team Spirit",
    teamB: "Team Liquid",
    matchResultTeamA: "2",
    matchResultTeamB: "0",
    winnerTeam: "Team Spirit",
    winningCondition: "Best of 3",
    records: [
      {
        matchRecordId: 3,
        recordDate: ISODate("2025-12-11"),
        startingSide: "CT",
        finalScore: "13-9",
        topHalfScore: "8-4",
        bottomHalfScore: "5-5",
        teamAResult: "Win",
        teamBResult: "Loss",
        map: "Anubis",
        playerStats: [
          { playerId: 11, nickname: "donk", teamName: "Team Spirit", kills: 24, deaths: 14, assists: 5, rating: 1.50, adr: 104.2, dpr: 0.64 },
          { playerId: 12, nickname: "sh1ro", teamName: "Team Spirit", kills: 19, deaths: 11, assists: 4, rating: 1.31, adr: 78.8, dpr: 0.50 },
          { playerId: 16, nickname: "Twistzz", teamName: "Team Liquid", kills: 16, deaths: 18, assists: 6, rating: 0.96, adr: 72.5, dpr: 0.82 }
        ]
      },
      {
        matchRecordId: 4,
        recordDate: ISODate("2025-12-11"),
        startingSide: "T",
        finalScore: "13-11",
        topHalfScore: "6-6",
        bottomHalfScore: "7-5",
        teamAResult: "Win",
        teamBResult: "Loss",
        map: "Dust 2",
        playerStats: [
          { playerId: 11, nickname: "donk", teamName: "Team Spirit", kills: 27, deaths: 16, assists: 4, rating: 1.46, adr: 96.5, dpr: 0.67 },
          { playerId: 16, nickname: "Twistzz", teamName: "Team Liquid", kills: 22, deaths: 18, assists: 5, rating: 1.15, adr: 84.9, dpr: 0.75 }
        ]
      }
    ]
  },
  {
    _id: 3,
    tournamentId: 1,
    matchDate: ISODate("2025-12-12"),
    stage: "Quarter-finals",
    teamA: "G2 Esports",
    teamB: "HEROIC",
    matchResultTeamA: "2",
    matchResultTeamB: "1",
    winnerTeam: "G2 Esports",
    winningCondition: "Best of 3",
    records: [
      {
        matchRecordId: 5,
        recordDate: ISODate("2025-12-12"),
        startingSide: "T",
        finalScore: "6-13",
        topHalfScore: "2-10",
        bottomHalfScore: "4-3",
        teamAResult: "Loss",
        teamBResult: "Win",
        map: "Ancient",
        playerStats: [
          { playerId: 21, nickname: "NiKo", teamName: "G2 Esports", kills: 14, deaths: 17, assists: 3, rating: 0.89, adr: 69.5, dpr: 0.89 }
        ]
      }
    ]
  },
  {
    _id: 4,
    tournamentId: 1,
    matchDate: ISODate("2025-12-12"),
    stage: "Quarter-finals",
    teamA: "Team Vitality",
    teamB: "FaZe Clan",
    matchResultTeamA: "1",
    matchResultTeamB: "2",
    winnerTeam: "FaZe Clan",
    winningCondition: "Best of 3",
    records: [
      {
        matchRecordId: 8,
        recordDate: ISODate("2025-12-12"),
        startingSide: "CT",
        finalScore: "13-6",
        topHalfScore: "10-2",
        bottomHalfScore: "3-4",
        teamAResult: "Win",
        teamBResult: "Loss",
        map: "Nuke",
        playerStats: [
          { playerId: 31, nickname: "ZywOo", teamName: "Team Vitality", kills: 25, deaths: 9, assists: 3, rating: 1.72, adr: 112.4, dpr: 0.47 },
          { playerId: 36, nickname: "ropz", teamName: "FaZe Clan", kills: 13, deaths: 18, assists: 5, rating: 0.83, adr: 66.2, dpr: 0.95 }
        ]
      }
    ]
  },
  {
    _id: 5,
    tournamentId: 1,
    matchDate: ISODate("2025-12-14"),
    stage: "Semi-finals",
    teamA: "MOUZ",
    teamB: "Team Spirit",
    matchResultTeamA: "1",
    matchResultTeamB: "2",
    winnerTeam: "Team Spirit",
    winningCondition: "Best of 3",
    records: [
      {
        matchRecordId: 11,
        recordDate: ISODate("2025-12-14"),
        startingSide: "CT",
        finalScore: "13-7",
        topHalfScore: "9-3",
        bottomHalfScore: "4-4",
        teamAResult: "Win",
        teamBResult: "Loss",
        map: "Nuke",
        playerStats: [
          { playerId: 6, nickname: "Brollan", teamName: "MOUZ", kills: 23, deaths: 12, assists: 6, rating: 1.42, adr: 97.5, dpr: 0.60 },
          { playerId: 11, nickname: "donk", teamName: "Team Spirit", kills: 18, deaths: 17, assists: 4, rating: 1.02, adr: 81.7, dpr: 0.85 }
        ]
      }
    ]
  },
  {
    _id: 6,
    tournamentId: 1,
    matchDate: ISODate("2025-12-14"),
    stage: "Semi-finals",
    teamA: "G2 Esports",
    teamB: "FaZe Clan",
    matchResultTeamA: "0",
    matchResultTeamB: "2",
    winnerTeam: "FaZe Clan",
    winningCondition: "Best of 3",
    records: [
      {
        matchRecordId: 14,
        recordDate: ISODate("2025-12-14"),
        startingSide: "T",
        finalScore: "6-13",
        topHalfScore: "3-9",
        bottomHalfScore: "3-4",
        teamAResult: "Loss",
        teamBResult: "Win",
        map: "Ancient",
        playerStats: [
          { playerId: 21, nickname: "NiKo", teamName: "G2 Esports", kills: 16, deaths: 16, assists: 5, rating: 1.00, adr: 76.1, dpr: 0.84 },
          { playerId: 36, nickname: "ropz", teamName: "FaZe Clan", kills: 22, deaths: 10, assists: 4, rating: 1.54, adr: 98.2, dpr: 0.53 }
        ]
      }
    ]
  },
  {
    _id: 7,
    tournamentId: 1,
    matchDate: ISODate("2025-12-15"),
    stage: "Grand Final",
    teamA: "Team Spirit",
    teamB: "FaZe Clan",
    matchResultTeamA: "2",
    matchResultTeamB: "1",
    winnerTeam: "Team Spirit",
    winningCondition: "Best of 3",
    records: [
      {
        matchRecordId: 16,
        recordDate: ISODate("2025-12-15"),
        startingSide: "CT",
        finalScore: "13-8",
        topHalfScore: "7-5",
        bottomHalfScore: "6-3",
        teamAResult: "Win",
        teamBResult: "Loss",
        map: "Nuke",
        playerStats: [
          { playerId: 11, nickname: "donk", teamName: "Team Spirit", kills: 25, deaths: 13, assists: 5, rating: 1.55, adr: 105.6, dpr: 0.62 },
          { playerId: 12, nickname: "sh1ro", teamName: "Team Spirit", kills: 20, deaths: 10, assists: 4, rating: 1.40, adr: 82.4, dpr: 0.48 },
          { playerId: 36, nickname: "ropz", teamName: "FaZe Clan", kills: 17, deaths: 19, assists: 5, rating: 0.98, adr: 73.1, dpr: 0.90 }
        ]
      },
      {
        matchRecordId: 17,
        recordDate: ISODate("2025-12-15"),
        startingSide: "T",
        finalScore: "6-13",
        topHalfScore: "2-10",
        bottomHalfScore: "4-3",
        teamAResult: "Loss",
        teamBResult: "Win",
        map: "Ancient",
        playerStats: [
          { playerId: 11, nickname: "donk", teamName: "Team Spirit", kills: 18, deaths: 18, assists: 2, rating: 0.98, adr: 79.3, dpr: 0.95 },
          { playerId: 36, nickname: "ropz", teamName: "FaZe Clan", kills: 24, deaths: 12, assists: 6, rating: 1.61, adr: 106.8, dpr: 0.63 }
        ]
      },
      {
        matchRecordId: 18,
        recordDate: ISODate("2025-12-15"),
        startingSide: "CT",
        finalScore: "13-11",
        topHalfScore: "6-6",
        bottomHalfScore: "7-5",
        teamAResult: "Win",
        teamBResult: "Loss",
        map: "Dust 2",
        playerStats: [
          { playerId: 11, nickname: "donk", teamName: "Team Spirit", kills: 30, deaths: 16, assists: 4, rating: 1.66, adr: 112.5, dpr: 0.67 },
          { playerId: 12, nickname: "sh1ro", teamName: "Team Spirit", kills: 21, deaths: 13, assists: 6, rating: 1.35, adr: 80.3, dpr: 0.54 },
          { playerId: 36, nickname: "ropz", teamName: "FaZe Clan", kills: 22, deaths: 20, assists: 3, rating: 1.08, adr: 86.7, dpr: 0.83 }
        ]
      }
    ]
  }
]);

db.matches.createIndex({ tournamentId: 1, stage: 1 }, { name: "idx_matches_tournament_stage" });
db.matches.createIndex({ matchDate: 1 }, { name: "idx_matches_match_date" });
db.matches.createIndex({ winnerTeam: 1 }, { name: "idx_matches_winner_team" });
db.matches.createIndex({ "records.playerStats.playerId": 1 }, { name: "idx_matches_player_stats_player" });
db.people.createIndex({ "contract.teamName": 1 }, { name: "idx_people_contract_team" });

print("MongoDB small Counter-Strike dataset loaded into database: " + dbName);
print("Collections: teams, people, tournaments, matches");
