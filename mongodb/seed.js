// MongoDB seed data for CS2 Database Browser
// Run with: mongosh mongodb://localhost:27017/cs2 mongodb/seed.js

// Clear existing data
db.people.drop();
db.player.drop();
db.coach.drop();
db.team.drop();
db.playercontract.drop();
db.coachcontract.drop();
db.tournament.drop();
db.matchtable.drop();
db.matchrecord.drop();
db.playermatchstats.drop();
db.tournamentparticipation.drop();

// ── Teams ───────────────────────────────────────────────────────────────────
db.team.insertMany([
    { TeamName: "The MongolZ",  Region: "Asia"     },
    { TeamName: "MOUZ",         Region: "Europe"   },
    { TeamName: "Team Spirit",  Region: "CIS"      },
    { TeamName: "Team Liquid",  Region: "Americas" },
    { TeamName: "G2 Esports",   Region: "Europe"   },
    { TeamName: "HEROIC",       Region: "Europe"   },
    { TeamName: "Team Vitality",Region: "Europe"   },
    { TeamName: "FaZe Clan",    Region: "Europe"   }
]);

// ── People ──────────────────────────────────────────────────────────────────
db.people.insertMany([
    // The MongolZ
    { PersonID: 1,  Nickname: "mzinho",    FullName: "Ayush Batbold",          Birthday: new Date("2007-05-31"), Nationality: "Mongolia" },
    { PersonID: 2,  Nickname: "Techno",    FullName: "Munkhbold Sodbayar",     Birthday: new Date("2005-05-18"), Nationality: "Mongolia" },
    { PersonID: 3,  Nickname: "910",       FullName: "Usukhbayar Banzragch",   Birthday: new Date("2002-12-19"), Nationality: "Mongolia" },
    { PersonID: 4,  Nickname: "bLitz",     FullName: "Garidmagnai Byambasuren",Birthday: new Date("2001-08-23"), Nationality: "Mongolia" },
    { PersonID: 5,  Nickname: "Senzu",     FullName: "Munkhbold Azbayar",      Birthday: new Date("2006-09-19"), Nationality: "Mongolia" },
    // MOUZ
    { PersonID: 6,  Nickname: "Brollan",   FullName: "Ludvig Brolin",          Birthday: new Date("2002-06-17"), Nationality: "Sweden"  },
    { PersonID: 7,  Nickname: "Jimpphat",  FullName: "Jimi Salo",              Birthday: new Date("2006-09-08"), Nationality: "Finland" },
    { PersonID: 8,  Nickname: "xertioN",   FullName: "Dorian Berman",          Birthday: new Date("2004-07-20"), Nationality: "Israel"  },
    { PersonID: 9,  Nickname: "siuhy",     FullName: "Kamil Szkaradek",        Birthday: new Date("2002-08-26"), Nationality: "Poland"  },
    { PersonID: 10, Nickname: "torzsi",    FullName: "Adam Torzsas",           Birthday: new Date("2002-05-22"), Nationality: "Hungary" },
    // Team Spirit
    { PersonID: 11, Nickname: "donk",      FullName: "Danil Kryshkovets",      Birthday: new Date("2007-01-25"), Nationality: "Russia"  },
    { PersonID: 12, Nickname: "sh1ro",     FullName: "Dmitry Sokolov",         Birthday: new Date("2001-07-15"), Nationality: "Russia"  },
    { PersonID: 13, Nickname: "chopper",   FullName: "Leonid Vishnyakov",      Birthday: new Date("1997-02-02"), Nationality: "Russia"  },
    { PersonID: 14, Nickname: "zont1x",    FullName: "Myroslav Plakhotia",     Birthday: new Date("2005-07-20"), Nationality: "Ukraine" },
    { PersonID: 15, Nickname: "magixx",    FullName: "Boris Vorobiev",         Birthday: new Date("2003-06-03"), Nationality: "Russia"  },
    // Team Liquid
    { PersonID: 16, Nickname: "Twistzz",   FullName: "Russel Van Dulken",      Birthday: new Date("1999-11-14"), Nationality: "Canada"    },
    { PersonID: 17, Nickname: "jks",       FullName: "Justin Savage",          Birthday: new Date("1995-12-12"), Nationality: "Australia" },
    { PersonID: 18, Nickname: "YEKINDAR",  FullName: "Mareks Galinskis",       Birthday: new Date("1999-10-04"), Nationality: "Latvia"    },
    { PersonID: 19, Nickname: "NAF",       FullName: "Keith Markovic",         Birthday: new Date("1997-11-24"), Nationality: "Canada"    },
    { PersonID: 20, Nickname: "ultimate",  FullName: "Roland Tomkowiak",       Birthday: new Date("2002-12-20"), Nationality: "Poland"    },
    // G2 Esports
    { PersonID: 21, Nickname: "NiKo",      FullName: "Nikola Kovac",           Birthday: new Date("1997-02-16"), Nationality: "Bosnia and Herzegovina" },
    { PersonID: 22, Nickname: "m0NESY",    FullName: "Ilya Osipov",            Birthday: new Date("2005-05-01"), Nationality: "Russia" },
    { PersonID: 23, Nickname: "malbsMd",   FullName: "Mario Samayoa",          Birthday: new Date("2002-11-06"), Nationality: "Guatemala" },
    { PersonID: 24, Nickname: "Snax",      FullName: "Janusz Pogorzelski",     Birthday: new Date("1993-07-05"), Nationality: "Poland" },
    { PersonID: 25, Nickname: "huNter-",   FullName: "Nemanja Kovac",          Birthday: new Date("1996-01-03"), Nationality: "Bosnia and Herzegovina" },
    // HEROIC
    { PersonID: 26, Nickname: "degster",   FullName: "Abdul Gasanov",          Birthday: new Date("2001-09-06"), Nationality: "Russia"         },
    { PersonID: 27, Nickname: "TeSeS",     FullName: "Rene Madsen",            Birthday: new Date("2001-03-24"), Nationality: "Denmark"        },
    { PersonID: 28, Nickname: "NertZ",     FullName: "Guy Iluz",               Birthday: new Date("1999-07-13"), Nationality: "Israel"         },
    { PersonID: 29, Nickname: "sjuush",    FullName: "Rasmus Beck",            Birthday: new Date("1999-01-04"), Nationality: "Denmark"        },
    { PersonID: 30, Nickname: "kyxsan",    FullName: "Damjan Stoilkovski",     Birthday: new Date("2000-01-18"), Nationality: "North Macedonia"},
    // Team Vitality
    { PersonID: 31, Nickname: "ZywOo",     FullName: "Mathieu Herbaut",        Birthday: new Date("2000-11-09"), Nationality: "France"         },
    { PersonID: 32, Nickname: "Spinx",     FullName: "Lotan Giladi",           Birthday: new Date("2000-09-13"), Nationality: "Israel"         },
    { PersonID: 33, Nickname: "flameZ",    FullName: "Shahar Shushan",         Birthday: new Date("2003-06-22"), Nationality: "Israel"         },
    { PersonID: 34, Nickname: "mezii",     FullName: "William Merriman",       Birthday: new Date("1998-10-15"), Nationality: "United Kingdom" },
    { PersonID: 35, Nickname: "apEX",      FullName: "Dan Madesclaire",        Birthday: new Date("1993-02-22"), Nationality: "France"         },
    // FaZe Clan
    { PersonID: 36, Nickname: "ropz",      FullName: "Robin Kool",             Birthday: new Date("1999-12-22"), Nationality: "Estonia" },
    { PersonID: 37, Nickname: "broky",     FullName: "Helvijs Saukants",       Birthday: new Date("2001-02-14"), Nationality: "Latvia"  },
    { PersonID: 38, Nickname: "frozen",    FullName: "David Cernansky",        Birthday: new Date("2002-07-18"), Nationality: "Slovakia"},
    { PersonID: 39, Nickname: "rain",      FullName: "Havard Nygaard",         Birthday: new Date("1994-08-27"), Nationality: "Norway"  },
    { PersonID: 40, Nickname: "karrigan",  FullName: "Finn Andersen",          Birthday: new Date("1990-04-14"), Nationality: "Denmark" },
    // Coaches
    { PersonID: 41, Nickname: "maaRaa",    FullName: "Erdenedalai Bayanbat",   Birthday: new Date("1989-01-21"), Nationality: "Mongolia" },
    { PersonID: 42, Nickname: "sycrone",   FullName: "Dennis Nielsen",         Birthday: new Date("1996-01-14"), Nationality: "Denmark"  },
    { PersonID: 43, Nickname: "hally",     FullName: "Sergey Shavaev",         Birthday: new Date("1993-01-11"), Nationality: "Russia"   },
    { PersonID: 44, Nickname: "mithR",     FullName: "Torbjorn Nyborg",        Birthday: new Date("1989-11-20"), Nationality: "Denmark"  },
    { PersonID: 45, Nickname: "TaZ",       FullName: "Wiktor Wojtas",          Birthday: new Date("1986-06-06"), Nationality: "Poland"   },
    { PersonID: 46, Nickname: "sAw",       FullName: "Eetu Saha",              Birthday: new Date("1992-05-18"), Nationality: "Finland"  },
    { PersonID: 47, Nickname: "XTQZZZ",    FullName: "Remy Quoniam",           Birthday: new Date("1988-12-23"), Nationality: "France"   },
    { PersonID: 48, Nickname: "NEO",       FullName: "Filip Kubski",           Birthday: new Date("1987-06-15"), Nationality: "Poland"   }
]);

// ── Players ──────────────────────────────────────────────────────────────────
db.player.insertMany([
    { PersonID: 1,  Rating: 1.01, ADR: 71.2, DPR: 0.69 },
    { PersonID: 2,  Rating: 1.04, ADR: 76.5, DPR: 0.72 },
    { PersonID: 3,  Rating: 1.09, ADR: 70.8, DPR: 0.60 },
    { PersonID: 4,  Rating: 1.06, ADR: 77.4, DPR: 0.71 },
    { PersonID: 5,  Rating: 1.10, ADR: 79.1, DPR: 0.74 },
    { PersonID: 6,  Rating: 1.11, ADR: 80.5, DPR: 0.69 },
    { PersonID: 7,  Rating: 1.14, ADR: 75.2, DPR: 0.62 },
    { PersonID: 8,  Rating: 1.12, ADR: 81.3, DPR: 0.71 },
    { PersonID: 9,  Rating: 1.00, ADR: 69.9, DPR: 0.70 },
    { PersonID: 10, Rating: 1.10, ADR: 71.4, DPR: 0.61 },
    { PersonID: 11, Rating: 1.32, ADR: 90.4, DPR: 0.66 },
    { PersonID: 12, Rating: 1.21, ADR: 76.6, DPR: 0.59 },
    { PersonID: 13, Rating: 0.96, ADR: 68.8, DPR: 0.73 },
    { PersonID: 14, Rating: 1.06, ADR: 75.5, DPR: 0.68 },
    { PersonID: 15, Rating: 1.02, ADR: 72.2, DPR: 0.70 },
    { PersonID: 16, Rating: 1.14, ADR: 79.5, DPR: 0.65 },
    { PersonID: 17, Rating: 1.08, ADR: 75.1, DPR: 0.66 },
    { PersonID: 18, Rating: 1.11, ADR: 80.8, DPR: 0.74 },
    { PersonID: 19, Rating: 1.13, ADR: 79.9, DPR: 0.64 },
    { PersonID: 20, Rating: 1.08, ADR: 73.3, DPR: 0.65 },
    { PersonID: 21, Rating: 1.20, ADR: 84.4, DPR: 0.70 },
    { PersonID: 22, Rating: 1.24, ADR: 76.2, DPR: 0.58 },
    { PersonID: 23, Rating: 1.11, ADR: 82.1, DPR: 0.76 },
    { PersonID: 24, Rating: 1.06, ADR: 78.4, DPR: 0.71 },
    { PersonID: 25, Rating: 1.08, ADR: 77.8, DPR: 0.69 },
    { PersonID: 26, Rating: 1.11, ADR: 75.4, DPR: 0.68 },
    { PersonID: 27, Rating: 1.05, ADR: 74.9, DPR: 0.70 },
    { PersonID: 28, Rating: 1.14, ADR: 80.6, DPR: 0.72 },
    { PersonID: 29, Rating: 1.04, ADR: 73.1, DPR: 0.68 },
    { PersonID: 30, Rating: 1.01, ADR: 69.5, DPR: 0.69 },
    { PersonID: 31, Rating: 1.31, ADR: 85.5, DPR: 0.61 },
    { PersonID: 32, Rating: 1.12, ADR: 79.8, DPR: 0.68 },
    { PersonID: 33, Rating: 1.09, ADR: 78.8, DPR: 0.71 },
    { PersonID: 34, Rating: 1.03, ADR: 73.4, DPR: 0.69 },
    { PersonID: 35, Rating: 1.01, ADR: 71.2, DPR: 0.74 },
    { PersonID: 36, Rating: 1.16, ADR: 78.5, DPR: 0.61 },
    { PersonID: 37, Rating: 1.11, ADR: 74.8, DPR: 0.64 },
    { PersonID: 38, Rating: 1.10, ADR: 79.4, DPR: 0.68 },
    { PersonID: 39, Rating: 1.05, ADR: 77.2, DPR: 0.71 },
    { PersonID: 40, Rating: 0.94, ADR: 66.8, DPR: 0.73 }
]);

// ── Coaches ──────────────────────────────────────────────────────────────────
db.coach.insertMany([
    { PersonID: 41, WinRate: 58.5 },
    { PersonID: 42, WinRate: 62.1 },
    { PersonID: 43, WinRate: 65.4 },
    { PersonID: 44, WinRate: 55.8 },
    { PersonID: 45, WinRate: 54.2 },
    { PersonID: 46, WinRate: 59.7 },
    { PersonID: 47, WinRate: 60.5 },
    { PersonID: 48, WinRate: 57.9 }
]);

// ── Player Contracts ─────────────────────────────────────────────────────────
db.playercontract.insertMany([
    { PlayerContractID: 1,  PersonID: 1,  TeamName: "The MongolZ",  StartDate: new Date("2023-03-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 2,  PersonID: 2,  TeamName: "The MongolZ",  StartDate: new Date("2023-03-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 3,  PersonID: 3,  TeamName: "The MongolZ",  StartDate: new Date("2023-03-01"), EndDate: null, InGameRole: "AWPer"         },
    { PlayerContractID: 4,  PersonID: 4,  TeamName: "The MongolZ",  StartDate: new Date("2023-03-01"), EndDate: null, InGameRole: "IGL"           },
    { PlayerContractID: 5,  PersonID: 5,  TeamName: "The MongolZ",  StartDate: new Date("2023-03-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 6,  PersonID: 6,  TeamName: "MOUZ",         StartDate: new Date("2023-12-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 7,  PersonID: 7,  TeamName: "MOUZ",         StartDate: new Date("2023-01-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 8,  PersonID: 8,  TeamName: "MOUZ",         StartDate: new Date("2021-06-01"), EndDate: null, InGameRole: "Entry Fragger" },
    { PlayerContractID: 9,  PersonID: 9,  TeamName: "MOUZ",         StartDate: new Date("2023-07-01"), EndDate: null, InGameRole: "IGL"           },
    { PlayerContractID: 10, PersonID: 10, TeamName: "MOUZ",         StartDate: new Date("2021-06-01"), EndDate: null, InGameRole: "AWPer"         },
    { PlayerContractID: 11, PersonID: 11, TeamName: "Team Spirit",  StartDate: new Date("2023-07-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 12, PersonID: 12, TeamName: "Team Spirit",  StartDate: new Date("2023-12-01"), EndDate: null, InGameRole: "AWPer"         },
    { PlayerContractID: 13, PersonID: 13, TeamName: "Team Spirit",  StartDate: new Date("2021-01-01"), EndDate: null, InGameRole: "IGL"           },
    { PlayerContractID: 14, PersonID: 14, TeamName: "Team Spirit",  StartDate: new Date("2023-07-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 15, PersonID: 15, TeamName: "Team Spirit",  StartDate: new Date("2019-09-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 16, PersonID: 16, TeamName: "Team Liquid",  StartDate: new Date("2023-12-01"), EndDate: null, InGameRole: "IGL"           },
    { PlayerContractID: 17, PersonID: 17, TeamName: "Team Liquid",  StartDate: new Date("2024-07-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 18, PersonID: 18, TeamName: "Team Liquid",  StartDate: new Date("2022-06-01"), EndDate: null, InGameRole: "Entry Fragger" },
    { PlayerContractID: 19, PersonID: 19, TeamName: "Team Liquid",  StartDate: new Date("2018-01-01"), EndDate: null, InGameRole: "Lurker"        },
    { PlayerContractID: 20, PersonID: 20, TeamName: "Team Liquid",  StartDate: new Date("2024-07-01"), EndDate: null, InGameRole: "AWPer"         },
    { PlayerContractID: 21, PersonID: 21, TeamName: "G2 Esports",   StartDate: new Date("2020-11-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 22, PersonID: 22, TeamName: "G2 Esports",   StartDate: new Date("2022-01-01"), EndDate: null, InGameRole: "AWPer"         },
    { PlayerContractID: 23, PersonID: 23, TeamName: "G2 Esports",   StartDate: new Date("2024-07-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 24, PersonID: 24, TeamName: "G2 Esports",   StartDate: new Date("2024-07-01"), EndDate: null, InGameRole: "IGL"           },
    { PlayerContractID: 25, PersonID: 25, TeamName: "G2 Esports",   StartDate: new Date("2020-11-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 26, PersonID: 26, TeamName: "HEROIC",       StartDate: new Date("2024-05-01"), EndDate: null, InGameRole: "AWPer"         },
    { PlayerContractID: 27, PersonID: 27, TeamName: "HEROIC",       StartDate: new Date("2019-09-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 28, PersonID: 28, TeamName: "HEROIC",       StartDate: new Date("2023-12-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 29, PersonID: 29, TeamName: "HEROIC",       StartDate: new Date("2021-02-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 30, PersonID: 30, TeamName: "HEROIC",       StartDate: new Date("2023-12-01"), EndDate: null, InGameRole: "IGL"           },
    { PlayerContractID: 31, PersonID: 31, TeamName: "Team Vitality", StartDate: new Date("2018-10-01"), EndDate: null, InGameRole: "AWPer"        },
    { PlayerContractID: 32, PersonID: 32, TeamName: "Team Vitality", StartDate: new Date("2022-08-01"), EndDate: null, InGameRole: "Rifler"       },
    { PlayerContractID: 33, PersonID: 33, TeamName: "Team Vitality", StartDate: new Date("2023-06-01"), EndDate: null, InGameRole: "Entry Fragger"},
    { PlayerContractID: 34, PersonID: 34, TeamName: "Team Vitality", StartDate: new Date("2023-11-01"), EndDate: null, InGameRole: "Lurker"       },
    { PlayerContractID: 35, PersonID: 35, TeamName: "Team Vitality", StartDate: new Date("2018-10-01"), EndDate: null, InGameRole: "IGL"          },
    { PlayerContractID: 36, PersonID: 36, TeamName: "FaZe Clan",    StartDate: new Date("2022-01-01"), EndDate: null, InGameRole: "Lurker"        },
    { PlayerContractID: 37, PersonID: 37, TeamName: "FaZe Clan",    StartDate: new Date("2019-09-01"), EndDate: null, InGameRole: "AWPer"         },
    { PlayerContractID: 38, PersonID: 38, TeamName: "FaZe Clan",    StartDate: new Date("2023-11-01"), EndDate: null, InGameRole: "Rifler"        },
    { PlayerContractID: 39, PersonID: 39, TeamName: "FaZe Clan",    StartDate: new Date("2016-01-01"), EndDate: null, InGameRole: "Entry Fragger" },
    { PlayerContractID: 40, PersonID: 40, TeamName: "FaZe Clan",    StartDate: new Date("2021-02-01"), EndDate: null, InGameRole: "IGL"           }
]);

// ── Coach Contracts ──────────────────────────────────────────────────────────
db.coachcontract.insertMany([
    { CoachContractID: 1, PersonID: 41, TeamName: "The MongolZ",   StartDate: new Date("2023-01-01"), EndDate: null },
    { CoachContractID: 2, PersonID: 42, TeamName: "MOUZ",          StartDate: new Date("2022-01-05"), EndDate: null },
    { CoachContractID: 3, PersonID: 43, TeamName: "Team Spirit",   StartDate: new Date("2022-02-01"), EndDate: null },
    { CoachContractID: 4, PersonID: 44, TeamName: "Team Liquid",   StartDate: new Date("2024-06-15"), EndDate: null },
    { CoachContractID: 5, PersonID: 45, TeamName: "G2 Esports",    StartDate: new Date("2023-12-11"), EndDate: null },
    { CoachContractID: 6, PersonID: 46, TeamName: "HEROIC",        StartDate: new Date("2023-12-15"), EndDate: null },
    { CoachContractID: 7, PersonID: 47, TeamName: "Team Vitality", StartDate: new Date("2023-10-13"), EndDate: null },
    { CoachContractID: 8, PersonID: 48, TeamName: "FaZe Clan",     StartDate: new Date("2023-07-27"), EndDate: null }
]);

// ── Tournament ───────────────────────────────────────────────────────────────
db.tournament.insertOne({
    TournamentID: 1,
    TournamentName: "Perfect World Shanghai Major 2025",
    TournyStartDate: new Date("2025-11-30"),
    TournyEndDate:   new Date("2025-12-15")
});

// ── Tournament Participation ─────────────────────────────────────────────────
db.tournamentparticipation.insertMany([
    { TournamentID: 1, TeamName: "Team Spirit",   Placement: "1st"   },
    { TournamentID: 1, TeamName: "FaZe Clan",     Placement: "2nd"   },
    { TournamentID: 1, TeamName: "G2 Esports",    Placement: "3-4th" },
    { TournamentID: 1, TeamName: "MOUZ",          Placement: "3-4th" },
    { TournamentID: 1, TeamName: "Team Vitality", Placement: "5-8th" },
    { TournamentID: 1, TeamName: "HEROIC",        Placement: "5-8th" },
    { TournamentID: 1, TeamName: "Team Liquid",   Placement: "5-8th" },
    { TournamentID: 1, TeamName: "The MongolZ",   Placement: "5-8th" }
]);

// ── Matches ──────────────────────────────────────────────────────────────────
db.matchtable.insertMany([
    { MatchID: 1, TournamentID: 1, MatchDate: new Date("2025-12-11"), Stage: "Quarter-finals", TeamA: "The MongolZ", TeamB: "MOUZ",         MatchResultTeamA: "0", MatchResultTeamB: "2", WinnerTeam: "MOUZ",         WinningCondition: "Best of 3" },
    { MatchID: 2, TournamentID: 1, MatchDate: new Date("2025-12-11"), Stage: "Quarter-finals", TeamA: "Team Spirit",  TeamB: "Team Liquid",  MatchResultTeamA: "2", MatchResultTeamB: "0", WinnerTeam: "Team Spirit",  WinningCondition: "Best of 3" },
    { MatchID: 3, TournamentID: 1, MatchDate: new Date("2025-12-12"), Stage: "Quarter-finals", TeamA: "G2 Esports",   TeamB: "HEROIC",       MatchResultTeamA: "2", MatchResultTeamB: "1", WinnerTeam: "G2 Esports",   WinningCondition: "Best of 3" },
    { MatchID: 4, TournamentID: 1, MatchDate: new Date("2025-12-12"), Stage: "Quarter-finals", TeamA: "Team Vitality", TeamB: "FaZe Clan",   MatchResultTeamA: "1", MatchResultTeamB: "2", WinnerTeam: "FaZe Clan",    WinningCondition: "Best of 3" },
    { MatchID: 5, TournamentID: 1, MatchDate: new Date("2025-12-14"), Stage: "Semi-finals",    TeamA: "MOUZ",         TeamB: "Team Spirit",  MatchResultTeamA: "1", MatchResultTeamB: "2", WinnerTeam: "Team Spirit",  WinningCondition: "Best of 3" },
    { MatchID: 6, TournamentID: 1, MatchDate: new Date("2025-12-14"), Stage: "Semi-finals",    TeamA: "G2 Esports",   TeamB: "FaZe Clan",   MatchResultTeamA: "0", MatchResultTeamB: "2", WinnerTeam: "FaZe Clan",    WinningCondition: "Best of 3" },
    { MatchID: 7, TournamentID: 1, MatchDate: new Date("2025-12-15"), Stage: "Grand Final",    TeamA: "Team Spirit",  TeamB: "FaZe Clan",   MatchResultTeamA: "2", MatchResultTeamB: "1", WinnerTeam: "Team Spirit",  WinningCondition: "Best of 3" }
]);

// ── Match Records (Maps) ─────────────────────────────────────────────────────
db.matchrecord.insertMany([
    { MatchRecordID: 1,  MatchID: 1, RecordDate: new Date("2025-12-11"), TeamA: "The MongolZ",  TeamB: "MOUZ",         StartingSide: "CT", FinalScore: "14-16", TopHalfScore: "7-5",  BottomHalfScore: "6-7", TeamAResult: "Loss", TeamBResult: "Win",  Map: "Ancient" },
    { MatchRecordID: 2,  MatchID: 1, RecordDate: new Date("2025-12-11"), TeamA: "The MongolZ",  TeamB: "MOUZ",         StartingSide: "T",  FinalScore: "9-13",  TopHalfScore: "4-8",  BottomHalfScore: "5-5", TeamAResult: "Loss", TeamBResult: "Win",  Map: "Nuke"    },
    { MatchRecordID: 3,  MatchID: 2, RecordDate: new Date("2025-12-11"), TeamA: "Team Spirit",  TeamB: "Team Liquid",  StartingSide: "CT", FinalScore: "13-9",  TopHalfScore: "8-4",  BottomHalfScore: "5-5", TeamAResult: "Win",  TeamBResult: "Loss", Map: "Anubis"  },
    { MatchRecordID: 4,  MatchID: 2, RecordDate: new Date("2025-12-11"), TeamA: "Team Spirit",  TeamB: "Team Liquid",  StartingSide: "T",  FinalScore: "13-11", TopHalfScore: "6-6",  BottomHalfScore: "7-5", TeamAResult: "Win",  TeamBResult: "Loss", Map: "Dust 2"  },
    { MatchRecordID: 5,  MatchID: 3, RecordDate: new Date("2025-12-12"), TeamA: "G2 Esports",   TeamB: "HEROIC",       StartingSide: "T",  FinalScore: "6-13",  TopHalfScore: "2-10", BottomHalfScore: "4-3", TeamAResult: "Loss", TeamBResult: "Win",  Map: "Ancient" },
    { MatchRecordID: 6,  MatchID: 3, RecordDate: new Date("2025-12-12"), TeamA: "G2 Esports",   TeamB: "HEROIC",       StartingSide: "CT", FinalScore: "13-9",  TopHalfScore: "9-3",  BottomHalfScore: "4-6", TeamAResult: "Win",  TeamBResult: "Loss", Map: "Nuke"    },
    { MatchRecordID: 7,  MatchID: 3, RecordDate: new Date("2025-12-12"), TeamA: "G2 Esports",   TeamB: "HEROIC",       StartingSide: "CT", FinalScore: "16-13", TopHalfScore: "6-6",  BottomHalfScore: "7-7", TeamAResult: "Win",  TeamBResult: "Loss", Map: "Mirage"  },
    { MatchRecordID: 16, MatchID: 7, RecordDate: new Date("2025-12-15"), TeamA: "Team Spirit",  TeamB: "FaZe Clan",    StartingSide: "CT", FinalScore: "13-8",  TopHalfScore: "7-5",  BottomHalfScore: "6-3", TeamAResult: "Win",  TeamBResult: "Loss", Map: "Nuke"    },
    { MatchRecordID: 17, MatchID: 7, RecordDate: new Date("2025-12-15"), TeamA: "Team Spirit",  TeamB: "FaZe Clan",    StartingSide: "T",  FinalScore: "6-13",  TopHalfScore: "2-10", BottomHalfScore: "4-3", TeamAResult: "Loss", TeamBResult: "Win",  Map: "Ancient" },
    { MatchRecordID: 18, MatchID: 7, RecordDate: new Date("2025-12-15"), TeamA: "Team Spirit",  TeamB: "FaZe Clan",    StartingSide: "CT", FinalScore: "13-11", TopHalfScore: "6-6",  BottomHalfScore: "7-5", TeamAResult: "Win",  TeamBResult: "Loss", Map: "Dust 2"  }
]);

print("MongoDB seed completed successfully.");
print("Collections created: people, player, coach, team, playercontract, coachcontract, tournament, matchtable, matchrecord, tournamentparticipation");
