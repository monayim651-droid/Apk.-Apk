package com.example.data.model

enum class SportType { FOOTBALL, CRICKET }

enum class MatchStatus { LIVE, UPCOMING, FINISHED }

data class TeamStub(
    val id: String,
    val name: String,
    val shortName: String,
    val logoUrl: String,
    val score: String = "",
    val subScore: String = ""
)

data class MatchSummary(
    val id: String,
    val sport: SportType,
    val leagueCode: String,
    val leagueName: String,
    val status: MatchStatus,
    val clockDisplay: String,
    val dateEpochMillis: Long,
    val venue: String,
    val home: TeamStub,
    val away: TeamStub,
    val statusNote: String = ""
)

data class MatchTimelineEvent(
    val minute: String,
    val title: String,
    val detail: String,
    val isHome: Boolean,
    val type: String
)

data class MatchStatRow(
    val label: String,
    val homeValue: String,
    val awayValue: String,
    val homeRatio: Float
)

data class PlayerLineupEntry(
    val id: String,
    val name: String,
    val number: String,
    val position: String,
    val rating: Float,
    val starter: Boolean = true,
    val heatZones: List<Float> = emptyList()
)

data class CricketBatterRow(
    val id: String,
    val name: String,
    val dismissal: String,
    val runs: Int,
    val balls: Int,
    val fours: Int,
    val sixes: Int,
    val strikeRate: Double
)

data class CricketBowlerRow(
    val id: String,
    val name: String,
    val overs: String,
    val maidens: Int,
    val runs: Int,
    val wickets: Int,
    val economy: Double
)

data class CricketFow(
    val wicketNumber: Int,
    val score: String,
    val batterName: String,
    val over: String
)

data class CricketOverPoint(
    val over: Int,
    val runsInOver: Int,
    val cumulativeRuns: Int
)

data class CricketInnings(
    val title: String,
    val totalScore: String,
    val runRate: String,
    val requiredRunRate: String = "",
    val powerplayStats: String = "",
    val batters: List<CricketBatterRow>,
    val bowlers: List<CricketBowlerRow>,
    val fallOfWickets: List<CricketFow>,
    val partnerships: List<String>,
    val overProgression: List<CricketOverPoint>
)

data class MatchDetail(
    val summary: MatchSummary,
    val commentary: List<String>,
    val timeline: List<MatchTimelineEvent>,
    val stats: List<MatchStatRow>,
    val homeFormation: String = "",
    val awayFormation: String = "",
    val homeLineup: List<PlayerLineupEntry> = emptyList(),
    val awayLineup: List<PlayerLineupEntry> = emptyList(),
    val homeManager: String = "",
    val awayManager: String = "",
    val stadiumCapacity: String = "",
    val tossInfo: String = "",
    val headToHeadSummary: String = "",
    val cricketInnings: List<CricketInnings> = emptyList(),
    val homeTeamRating: Float = 0f,
    val awayTeamRating: Float = 0f
)

data class LeagueStandingRow(
    val rank: Int,
    val teamId: String,
    val teamName: String,
    val teamLogo: String,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalDiff: String,
    val points: Int,
    val form: String
)

data class IccRankingRow(
    val rank: Int,
    val format: String,
    val category: String,
    val name: String,
    val team: String,
    val ratingPoints: Int
)

data class LeaderStatRow(
    val rank: Int,
    val playerId: String,
    val playerName: String,
    val teamName: String,
    val category: String,
    val value: String
)

data class TeamProfile(
    val id: String,
    val sport: SportType,
    val name: String,
    val country: String,
    val foundedYear: String,
    val stadium: String,
    val coach: String,
    val logoUrl: String,
    val biography: String,
    val recentForm: String,
    val recordSummary: String,
    val leaguePosition: String,
    val teamRating: Float,
    val achievements: List<String>,
    val squad: List<PlayerLineupEntry>
)

data class PlayerProfile(
    val id: String,
    val sport: SportType,
    val fullName: String,
    val photoUrl: String,
    val biography: String,
    val dateOfBirth: String,
    val nationality: String,
    val roleOrPosition: String,
    val currentTeam: String,
    val previousTeams: List<String>,
    val jerseyNumber: String,
    val rating: Float,
    val recentForm: List<Float>,
    val seasonStats: Map<String, String>,
    val careerStats: Map<String, String>,
    val awards: List<String>
)

data class SportsNewsItem(
    val id: String,
    val sport: SportType,
    val category: String,
    val headline: String,
    val description: String,
    val imageUrl: String,
    val publishedAt: String,
    val sourceUrl: String
)
