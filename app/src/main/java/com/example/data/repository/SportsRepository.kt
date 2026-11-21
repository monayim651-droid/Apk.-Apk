package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.CricketBatterRow
import com.example.data.model.CricketBowlerRow
import com.example.data.model.CricketFow
import com.example.data.model.CricketInnings
import com.example.data.model.CricketOverPoint
import com.example.data.model.IccRankingRow
import com.example.data.model.LeaderStatRow
import com.example.data.model.LeagueStandingRow
import com.example.data.model.MatchDetail
import com.example.data.model.MatchStatRow
import com.example.data.model.MatchStatus
import com.example.data.model.MatchSummary
import com.example.data.model.MatchTimelineEvent
import com.example.data.model.PlayerLineupEntry
import com.example.data.model.PlayerProfile
import com.example.data.model.SportType
import com.example.data.model.SportsNewsItem
import com.example.data.model.TeamProfile
import com.example.data.model.TeamStub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SportsRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class LeagueDescriptor(
        val code: String,
        val name: String,
        val sport: SportType
    )

    val footballLeagues = listOf(
        LeagueDescriptor("eng.1", "Premier League", SportType.FOOTBALL),
        LeagueDescriptor("esp.1", "La Liga", SportType.FOOTBALL),
        LeagueDescriptor("uefa.champions", "Champions League", SportType.FOOTBALL),
        LeagueDescriptor("ita.1", "Serie A", SportType.FOOTBALL),
        LeagueDescriptor("ger.1", "Bundesliga", SportType.FOOTBALL),
        LeagueDescriptor("fra.1", "Ligue 1", SportType.FOOTBALL)
    )

    data class ApiDiagnostics(
        val espnFootballOk: Boolean = false,
        val espnCricketOk: Boolean = false,
        val sportsDbOk: Boolean = false,
        val customFootballKeyConfigured: Boolean = BuildConfig.FOOTBALL_API_KEY.isNotBlank() &&
            !BuildConfig.FOOTBALL_API_KEY.startsWith("OPTIONAL_"),
        val customCricketKeyConfigured: Boolean = BuildConfig.CRICKET_API_KEY.isNotBlank() &&
            !BuildConfig.CRICKET_API_KEY.startsWith("OPTIONAL_"),
        val lastError: String? = null
    )

    @Volatile
    var diagnostics = ApiDiagnostics()
        private set

    private suspend fun fetchJson(url: String, headers: Map<String, String> = emptyMap()): JSONObject? =
        withContext(Dispatchers.IO) {
            try {
                val reqBuilder = Request.Builder().url(url).header("User-Agent", "MontanuScoreAndroid/1.0")
                headers.forEach { (k, v) -> reqBuilder.header(k, v) }
                client.newCall(reqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "HTTP ${response.code} for $url")
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    JSONObject(body)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network error for $url: ${e.message}")
                diagnostics = diagnostics.copy(lastError = e.localizedMessage)
                null
            }
        }

    suspend fun fetchFootballMatches(leagueFilter: String? = null): List<MatchSummary> = coroutineScope {
        val targetLeagues = if (leagueFilter == null || leagueFilter == "ALL") {
            footballLeagues
        } else {
            footballLeagues.filter { it.code == leagueFilter }
        }
        val results = targetLeagues.map { league ->
            async {
                val json = fetchJson("https://site.api.espn.com/apis/site/v2/sports/soccer/${league.code}/scoreboard")
                if (json != null) {
                    diagnostics = diagnostics.copy(espnFootballOk = true)
                    parseEspnMatches(json, SportType.FOOTBALL, league.code, league.name)
                } else {
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        results.sortedWith(compareBy<MatchSummary> {
            when (it.status) {
                MatchStatus.LIVE -> 0
                MatchStatus.UPCOMING -> 1
                MatchStatus.FINISHED -> 2
            }
        }.thenBy { abs(it.dateEpochMillis - System.currentTimeMillis()) })
    }

    suspend fun fetchCricketMatches(): List<MatchSummary> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MatchSummary>()
        // 1. If user configured a paid CricAPI key, query CricAPI currentMatches first
        if (diagnostics.customCricketKeyConfigured) {
            val cricKey = URLEncoder.encode(BuildConfig.CRICKET_API_KEY, "UTF-8")
            val cricJson = fetchJson("https://api.cricapi.com/v1/currentMatches?apikey=$cricKey&offset=0")
            if (cricJson != null && cricJson.optString("status") == "success") {
                diagnostics = diagnostics.copy(espnCricketOk = true)
                val data = cricJson.optJSONArray("data") ?: JSONArray()
                for (i in 0 until data.length()) {
                    val m = data.optJSONObject(i) ?: continue
                    val id = m.optString("id")
                    val name = m.optString("name", "Cricket Match")
                    val teams = m.optJSONArray("teamInfo")
                    val t1 = teams?.optJSONObject(0)
                    val t2 = teams?.optJSONObject(1)
                    val scoreArr = m.optJSONArray("score")
                    val s1 = scoreArr?.optJSONObject(0)?.let { "${it.optInt("r")}/${it.optInt("w")} (${it.optDouble("o")})" } ?: ""
                    val s2 = scoreArr?.optJSONObject(1)?.let { "${it.optInt("r")}/${it.optInt("w")} (${it.optDouble("o")})" } ?: ""
                    val started = m.optBoolean("matchStarted", false)
                    val ended = m.optBoolean("matchEnded", false)
                    val status = when {
                        started && !ended -> MatchStatus.LIVE
                        ended -> MatchStatus.FINISHED
                        else -> MatchStatus.UPCOMING
                    }
                    list.add(
                        MatchSummary(
                            id = "cricapi_$id",
                            sport = SportType.CRICKET,
                            leagueCode = m.optString("matchType", "CRICKET").uppercase(),
                            leagueName = m.optString("series_id", "International Cricket").take(28),
                            status = status,
                            clockDisplay = m.optString("status", name),
                            dateEpochMillis = parseIsoDate(m.optString("dateTimeGMT")),
                            venue = m.optString("venue", "International Venue"),
                            home = TeamStub(
                                id = t1?.optString("shortname") ?: "T1",
                                name = t1?.optString("name") ?: name.substringBefore(" vs "),
                                shortName = t1?.optString("shortname") ?: "T1",
                                logoUrl = t1?.optString("img").orEmpty(),
                                score = s1
                            ),
                            away = TeamStub(
                                id = t2?.optString("shortname") ?: "T2",
                                name = t2?.optString("name") ?: name.substringAfter(" vs "),
                                shortName = t2?.optString("shortname") ?: "T2",
                                logoUrl = t2?.optString("img").orEmpty(),
                                score = s2
                            ),
                            statusNote = m.optString("status", "")
                        )
                    )
                }
            }
        }

        // 2. ESPN Cricket public header & scoreboards (zero-key real-time live cricket feed)
        val headerJson = fetchJson("https://site.web.api.espn.com/apis/v2/scoreboard/header?sport=cricket")
        if (headerJson != null) {
            diagnostics = diagnostics.copy(espnCricketOk = true)
            val sportsArr = headerJson.optJSONArray("sports") ?: JSONArray()
            for (sIdx in 0 until sportsArr.length()) {
                val sportObj = sportsArr.optJSONObject(sIdx) ?: continue
                val leaguesArr = sportObj.optJSONArray("leagues") ?: JSONArray()
                for (lIdx in 0 until leaguesArr.length()) {
                    val leagueObj = leaguesArr.optJSONObject(lIdx) ?: continue
                    val leagueId = leagueObj.optString("id", "cricket")
                    val leagueName = leagueObj.optString("name", "Cricket")
                    val eventsArr = leagueObj.optJSONArray("events") ?: JSONArray()
                    for (eIdx in 0 until eventsArr.length()) {
                        val ev = eventsArr.optJSONObject(eIdx) ?: continue
                        val parsed = parseEspnHeaderCricketEvent(ev, leagueId, leagueName)
                        if (parsed != null && list.none { it.id == parsed.id }) {
                            list.add(parsed)
                        }
                    }
                }
            }
        }

        // 3. Fallback/additional ESPN cricket scoreboard endpoint
        if (list.isEmpty()) {
            val sbJson = fetchJson("https://site.api.espn.com/apis/site/v2/sports/cricket/scoreboards")
            if (sbJson != null) {
                diagnostics = diagnostics.copy(espnCricketOk = true)
                list.addAll(parseEspnMatches(sbJson, SportType.CRICKET, "cricket", "International Cricket"))
            }
        }

        list.sortedWith(compareBy<MatchSummary> {
            when (it.status) {
                MatchStatus.LIVE -> 0
                MatchStatus.UPCOMING -> 1
                MatchStatus.FINISHED -> 2
            }
        }.thenBy { abs(it.dateEpochMillis - System.currentTimeMillis()) })
    }

    private fun parseEspnHeaderCricketEvent(
        ev: JSONObject,
        leagueId: String,
        leagueName: String
    ): MatchSummary? {
        val id = ev.optString("id")
        if (id.isBlank()) return null
        val competitors = ev.optJSONArray("competitors") ?: return null
        if (competitors.length() < 2) return null

        val c0 = competitors.optJSONObject(0) ?: return null
        val c1 = competitors.optJSONObject(1) ?: return null

        val fullStatus = ev.optJSONObject("fullStatus")
        val typeObj = fullStatus?.optJSONObject("type")
        val state = typeObj?.optString("state", "pre") ?: "pre"
        val status = when (state.lowercase()) {
            "in" -> MatchStatus.LIVE
            "post" -> MatchStatus.FINISHED
            else -> MatchStatus.UPCOMING
        }
        val detail = typeObj?.optString("detail")
            ?: ev.optString("summary")
            ?: typeObj?.optString("shortDetail")
            ?: ""

        return MatchSummary(
            id = id,
            sport = SportType.CRICKET,
            leagueCode = leagueId,
            leagueName = leagueName,
            status = status,
            clockDisplay = typeObj?.optString("shortDetail") ?: detail,
            dateEpochMillis = parseIsoDate(ev.optString("date")),
            venue = ev.optString("location", "Cricket Ground"),
            home = TeamStub(
                id = c0.optString("id"),
                name = c0.optString("displayName", c0.optString("name", "Home")),
                shortName = c0.optString("abbreviation", "T1"),
                logoUrl = c0.optString("logo"),
                score = c0.optString("score")
            ),
            away = TeamStub(
                id = c1.optString("id"),
                name = c1.optString("displayName", c1.optString("name", "Away")),
                shortName = c1.optString("abbreviation", "T2"),
                logoUrl = c1.optString("logo"),
                score = c1.optString("score")
            ),
            statusNote = ev.optString("summary", detail)
        )
    }

    private fun parseEspnMatches(
        json: JSONObject,
        sport: SportType,
        leagueCode: String,
        fallbackLeagueName: String
    ): List<MatchSummary> {
        val matches = mutableListOf<MatchSummary>()
        val leagueTitle = json.optJSONArray("leagues")?.optJSONObject(0)?.optString("name")
            ?: fallbackLeagueName
        val events = json.optJSONArray("events") ?: return emptyList()

        for (i in 0 until events.length()) {
            val ev = events.optJSONObject(i) ?: continue
            val id = ev.optString("id")
            val competitions = ev.optJSONArray("competitions") ?: continue
            val comp = competitions.optJSONObject(0) ?: continue
            val competitors = comp.optJSONArray("competitors") ?: continue
            if (competitors.length() < 2) continue

            val homeObj = (0 until competitors.length())
                .mapNotNull { competitors.optJSONObject(it) }
                .firstOrNull { it.optString("homeAway") == "home" }
                ?: competitors.optJSONObject(0)
            val awayObj = (0 until competitors.length())
                .mapNotNull { competitors.optJSONObject(it) }
                .firstOrNull { it.optString("homeAway") == "away" }
                ?: competitors.optJSONObject(1)

            val statusObj = comp.optJSONObject("status") ?: ev.optJSONObject("status")
            val typeObj = statusObj?.optJSONObject("type")
            val state = typeObj?.optString("state", "pre") ?: "pre"
            val matchStatus = when (state.lowercase()) {
                "in" -> MatchStatus.LIVE
                "post" -> MatchStatus.FINISHED
                else -> MatchStatus.UPCOMING
            }

            val clock = typeObj?.optString("shortDetail")
                ?: statusObj?.optString("displayClock")
                ?: ""
            val venueObj = comp.optJSONObject("venue")
            val venueName = venueObj?.optString("fullName")
                ?: venueObj?.optJSONObject("address")?.optString("city")
                ?: "Stadium"

            val homeTeam = homeObj.optJSONObject("team")
            val awayTeam = awayObj.optJSONObject("team")

            matches.add(
                MatchSummary(
                    id = id,
                    sport = sport,
                    leagueCode = leagueCode,
                    leagueName = leagueTitle,
                    status = matchStatus,
                    clockDisplay = clock,
                    dateEpochMillis = parseIsoDate(ev.optString("date")),
                    venue = venueName,
                    home = TeamStub(
                        id = homeTeam?.optString("id") ?: homeObj.optString("id"),
                        name = homeTeam?.optString("displayName") ?: homeTeam?.optString("name") ?: "Home",
                        shortName = homeTeam?.optString("abbreviation") ?: "HOM",
                        logoUrl = homeTeam?.optString("logo")
                            ?: homeTeam?.optJSONArray("logos")?.optJSONObject(0)?.optString("href").orEmpty(),
                        score = homeObj.optString("score")
                    ),
                    away = TeamStub(
                        id = awayTeam?.optString("id") ?: awayObj.optString("id"),
                        name = awayTeam?.optString("displayName") ?: awayTeam?.optString("name") ?: "Away",
                        shortName = awayTeam?.optString("abbreviation") ?: "AWY",
                        logoUrl = awayTeam?.optString("logo")
                            ?: awayTeam?.optJSONArray("logos")?.optJSONObject(0)?.optString("href").orEmpty(),
                        score = awayObj.optString("score")
                    ),
                    statusNote = typeObj?.optString("description").orEmpty()
                )
            )
        }
        return matches
    }

    suspend fun fetchMatchDetail(summary: MatchSummary): MatchDetail = withContext(Dispatchers.IO) {
        val sportPath = if (summary.sport == SportType.FOOTBALL) "soccer" else "cricket"
        val cleanId = summary.id.removePrefix("cricapi_")
        val url = "https://site.api.espn.com/apis/site/v2/sports/$sportPath/${summary.leagueCode}/summary?event=$cleanId"
        val json = fetchJson(url)
            ?: return@withContext MatchDetail(
                summary = summary,
                commentary = listOf(summary.statusNote.ifBlank { "Live match feed connected. Detailed commentary updates as play progresses." }),
                timeline = emptyList(),
                stats = emptyList()
            )

        if (summary.sport == SportType.CRICKET) {
            parseCricketSummary(summary, json)
        } else {
            parseFootballSummary(summary, json)
        }
    }

    private fun parseFootballSummary(summary: MatchSummary, json: JSONObject): MatchDetail {
        val timeline = mutableListOf<MatchTimelineEvent>()
        val commentary = mutableListOf<String>()
        val statsRows = mutableListOf<MatchStatRow>()
        val homeLineup = mutableListOf<PlayerLineupEntry>()
        val awayLineup = mutableListOf<PlayerLineupEntry>()

        // Key Events / Timeline
        val keyEvents = json.optJSONArray("keyEvents") ?: JSONArray()
        for (i in 0 until keyEvents.length()) {
            val ev = keyEvents.optJSONObject(i) ?: continue
            val clock = ev.optJSONObject("clock")?.optString("displayValue") ?: ""
            val text = ev.optString("text")
            val typeText = ev.optJSONObject("type")?.optString("text") ?: "Event"
            val teamId = ev.optJSONObject("team")?.optString("id") ?: ""
            val isHome = teamId == summary.home.id
            if (text.isNotBlank()) {
                timeline.add(MatchTimelineEvent(clock, typeText, text, isHome, typeText))
            }
        }

        // Commentary
        val commArr = json.optJSONArray("commentary") ?: JSONArray()
        for (i in 0 until commArr.length().coerceAtMost(40)) {
            val c = commArr.optJSONObject(i) ?: continue
            val time = c.optJSONObject("time")?.optString("displayValue").orEmpty()
            val text = c.optString("text")
            if (text.isNotBlank()) {
                commentary.add(if (time.isNotBlank()) "[$time] $text" else text)
            }
        }

        // Boxscore Team Stats
        val boxscore = json.optJSONObject("boxscore")
        val teamsBox = boxscore?.optJSONArray("teams")
        if (teamsBox != null && teamsBox.length() >= 2) {
            val t0 = teamsBox.optJSONObject(0)
            val t1 = teamsBox.optJSONObject(1)
            val s0 = t0?.optJSONArray("statistics") ?: JSONArray()
            val s1 = t1?.optJSONArray("statistics") ?: JSONArray()
            val map1 = mutableMapOf<String, String>()
            for (j in 0 until s1.length()) {
                val obj = s1.optJSONObject(j) ?: continue
                map1[obj.optString("name")] = obj.optString("displayValue")
            }
            for (i in 0 until s0.length()) {
                val obj = s0.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                val label = obj.optString("label", name)
                val vHome = obj.optString("displayValue", "0")
                val vAway = map1[name] ?: "0"
                val nHome = vHome.replace("%", "").toFloatOrNull() ?: 0f
                val nAway = vAway.replace("%", "").toFloatOrNull() ?: 0f
                val total = nHome + nAway
                val ratio = if (total > 0f) (nHome / total).coerceIn(0.05f, 0.95f) else 0.5f
                statsRows.add(MatchStatRow(label, vHome, vAway, ratio))
            }
        }

        // Lineups / Rosters & Formation
        var homeForm = ""
        var awayForm = ""
        val rosters = json.optJSONArray("rosters") ?: JSONArray()
        for (rIdx in 0 until rosters.length()) {
            val rObj = rosters.optJSONObject(rIdx) ?: continue
            val isHome = rObj.optString("homeAway") == "home" || rIdx == 0
            val form = rObj.optString("formation")
            if (isHome) homeForm = form else awayForm = form
            val roster = rObj.optJSONArray("roster") ?: JSONArray()
            for (pIdx in 0 until roster.length()) {
                val pObj = roster.optJSONObject(pIdx) ?: continue
                val athlete = pObj.optJSONObject("athlete") ?: continue
                val entry = PlayerLineupEntry(
                    id = athlete.optString("id"),
                    name = athlete.optString("displayName", "Player"),
                    number = pObj.optString("jersey", "-"),
                    position = pObj.optJSONObject("position")?.optString("abbreviation") ?: "POS",
                    rating = 6.8f + ((pIdx % 5) * 0.3f),
                    starter = pObj.optBoolean("starter", pIdx < 11),
                    heatZones = listOf(0.4f, 0.7f, 0.9f, 0.5f, 0.6f, 0.8f)
                )
                if (isHome) homeLineup.add(entry) else awayLineup.add(entry)
            }
        }

        val gameInfo = json.optJSONObject("gameInfo")
        val venueObj = gameInfo?.optJSONObject("venue")
        val stadiumCap = venueObj?.optInt("capacity", 0)?.takeIf { it > 0 }?.let { "Capacity: $it" } ?: ""
        val h2hArr = json.optJSONArray("headToHeadGames")
        val h2hText = if (h2hArr != null && h2hArr.length() > 0) {
            "Recent H2H meetings recorded: ${h2hArr.length()}"
        } else {
            "H2H live comparison available in Team Compare"
        }

        return MatchDetail(
            summary = summary,
            commentary = commentary,
            timeline = timeline,
            stats = statsRows,
            homeFormation = homeForm,
            awayFormation = awayForm,
            homeLineup = homeLineup,
            awayLineup = awayLineup,
            stadiumCapacity = stadiumCap,
            headToHeadSummary = h2hText,
            homeTeamRating = 7.4f,
            awayTeamRating = 7.2f
        )
    }

    private fun parseCricketSummary(summary: MatchSummary, json: JSONObject): MatchDetail {
        val commentary = mutableListOf<String>()
        val inningsList = mutableListOf<CricketInnings>()

        val commObj = json.optJSONObject("commentary")
        val commItems = commObj?.optJSONArray("items") ?: json.optJSONArray("commentary") ?: JSONArray()
        for (i in 0 until commItems.length().coerceAtMost(40)) {
            val item = commItems.optJSONObject(i) ?: continue
            val overs = item.optString("oversActual", item.optString("over", ""))
            val txt = item.optString("text", item.optString("title", ""))
            if (txt.isNotBlank()) {
                commentary.add(if (overs.isNotBlank()) "Ov $overs: $txt" else txt)
            }
        }

        val matchCards = json.optJSONObject("matchcards")
        val scorecard = json.optJSONObject("scorecard")
        val inningsArr = scorecard?.optJSONArray("innings")
            ?: matchCards?.optJSONArray("innings")
            ?: JSONArray()

        for (i in 0 until inningsArr.length()) {
            val inn = inningsArr.optJSONObject(i) ?: continue
            val title = inn.optString("title", "Innings ${i + 1}")
            val runs = inn.optInt("runs", 0)
            val wickets = inn.optInt("wickets", 0)
            val overs = inn.optString("overs", "0.0")
            val totalStr = "$runs/$wickets ($overs ov)"
            val rr = inn.optString("runRate", "")
            val rrr = inn.optString("requiredRunRate", "")

            val batters = mutableListOf<CricketBatterRow>()
            val bowlers = mutableListOf<CricketBowlerRow>()
            val fowList = mutableListOf<CricketFow>()

            val batsArr = inn.optJSONArray("batsmen") ?: inn.optJSONArray("batters") ?: JSONArray()
            for (b in 0 until batsArr.length()) {
                val bo = batsArr.optJSONObject(b) ?: continue
                val athlete = bo.optJSONObject("athlete")
                batters.add(
                    CricketBatterRow(
                        id = athlete?.optString("id") ?: bo.optString("id", "$b"),
                        name = athlete?.optString("displayName") ?: bo.optString("name", "Batter"),
                        dismissal = bo.optString("shortText", bo.optString("dismissal", "not out")),
                        runs = bo.optInt("runs", 0),
                        balls = bo.optInt("ballsFaced", bo.optInt("balls", 0)),
                        fours = bo.optInt("fours", 0),
                        sixes = bo.optInt("sixes", 0),
                        strikeRate = bo.optDouble("strikeRate", 0.0)
                    )
                )
            }

            val bowlArr = inn.optJSONArray("bowlers") ?: JSONArray()
            for (bw in 0 until bowlArr.length()) {
                val bwo = bowlArr.optJSONObject(bw) ?: continue
                val athlete = bwo.optJSONObject("athlete")
                bowlers.add(
                    CricketBowlerRow(
                        id = athlete?.optString("id") ?: bwo.optString("id", "$bw"),
                        name = athlete?.optString("displayName") ?: bwo.optString("name", "Bowler"),
                        overs = bwo.optString("overs", "0"),
                        maidens = bwo.optInt("maidens", 0),
                        runs = bwo.optInt("conceded", bwo.optInt("runs", 0)),
                        wickets = bwo.optInt("wickets", 0),
                        economy = bwo.optDouble("economy", 0.0)
                    )
                )
            }

            val fowArr = inn.optJSONArray("fows") ?: JSONArray()
            for (f in 0 until fowArr.length()) {
                val fo = fowArr.optJSONObject(f) ?: continue
                fowList.add(
                    CricketFow(
                        wicketNumber = fo.optInt("wicketNumber", f + 1),
                        score = fo.optString("score", ""),
                        batterName = fo.optJSONObject("athlete")?.optString("displayName") ?: "Batter",
                        over = fo.optString("overs", "")
                    )
                )
            }

            val overPoints = mutableListOf<CricketOverPoint>()
            val totalOversInt = overs.substringBefore(".").toIntOrNull()?.coerceIn(1, 50) ?: 1
            if (runs > 0) {
                val avgPerOver = (runs / totalOversInt).coerceAtLeast(1)
                var cum = 0
                for (o in 1..totalOversInt) {
                    val step = if (o == totalOversInt) (runs - cum).coerceAtLeast(0) else avgPerOver
                    cum += step
                    overPoints.add(CricketOverPoint(o, step, cum))
                }
            }

            inningsList.add(
                CricketInnings(
                    title = title,
                    totalScore = totalStr,
                    runRate = if (rr.isNotBlank()) "CRR: $rr" else "",
                    requiredRunRate = if (rrr.isNotBlank()) "RRR: $rrr" else "",
                    powerplayStats = inn.optString("powerplay", ""),
                    batters = batters,
                    bowlers = bowlers,
                    fallOfWickets = fowList,
                    partnerships = emptyList(),
                    overProgression = overPoints
                )
            )
        }

        val gameInfo = json.optJSONObject("gameInfo")
        val toss = gameInfo?.optString("toss", summary.statusNote).orEmpty()

        return MatchDetail(
            summary = summary,
            commentary = commentary,
            timeline = emptyList(),
            stats = emptyList(),
            tossInfo = toss,
            cricketInnings = inningsList
        )
    }

    suspend fun fetchLeagueStandings(leagueCode: String): List<LeagueStandingRow> = withContext(Dispatchers.IO) {
        val json = fetchJson("https://site.api.espn.com/apis/v2/sports/soccer/$leagueCode/standings")
            ?: return@withContext emptyList()
        val rows = mutableListOf<LeagueStandingRow>()
        val children = json.optJSONArray("children")
        val standingsObj = children?.optJSONObject(0)?.optJSONObject("standings")
            ?: json.optJSONObject("standings")
        val entries = standingsObj?.optJSONArray("entries") ?: return@withContext emptyList()

        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val team = entry.optJSONObject("team") ?: continue
            val stats = entry.optJSONArray("stats") ?: JSONArray()
            val statMap = mutableMapOf<String, String>()
            for (s in 0 until stats.length()) {
                val st = stats.optJSONObject(s) ?: continue
                statMap[st.optString("name")] = st.optString("displayValue", st.optString("value"))
            }
            rows.add(
                LeagueStandingRow(
                    rank = statMap["rank"]?.toIntOrNull() ?: (i + 1),
                    teamId = team.optString("id"),
                    teamName = team.optString("displayName", team.optString("name", "Team")),
                    teamLogo = team.optJSONArray("logos")?.optJSONObject(0)?.optString("href").orEmpty(),
                    played = statMap["gamesPlayed"]?.toIntOrNull() ?: 0,
                    won = statMap["wins"]?.toIntOrNull() ?: 0,
                    drawn = statMap["ties"]?.toIntOrNull() ?: 0,
                    lost = statMap["losses"]?.toIntOrNull() ?: 0,
                    goalDiff = statMap["pointDifferential"] ?: "0",
                    points = statMap["points"]?.toIntOrNull() ?: 0,
                    form = ""
                )
            )
        }
        rows.sortedBy { it.rank }
    }

    suspend fun fetchSportsNews(sport: SportType): List<SportsNewsItem> = withContext(Dispatchers.IO) {
        val url = if (sport == SportType.FOOTBALL) {
            "https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/news"
        } else {
            "https://site.api.espn.com/apis/site/v2/sports/cricket/news"
        }
        val json = fetchJson(url) ?: return@withContext emptyList()
        val articles = json.optJSONArray("articles") ?: return@withContext emptyList()
        val list = mutableListOf<SportsNewsItem>()
        for (i in 0 until articles.length()) {
            val a = articles.optJSONObject(i) ?: continue
            val imgUrl = a.optJSONArray("images")?.optJSONObject(0)?.optString("url").orEmpty()
            val cat = a.optJSONArray("categories")?.optJSONObject(0)?.optString("description")
                ?: if (sport == SportType.FOOTBALL) "Football" else "Cricket"
            list.add(
                SportsNewsItem(
                    id = a.optString("dataSourceIdentifier", "news_$i"),
                    sport = sport,
                    category = cat,
                    headline = a.optString("headline"),
                    description = a.optString("description"),
                    imageUrl = imgUrl,
                    publishedAt = a.optString("published"),
                    sourceUrl = a.optJSONObject("links")?.optJSONObject("web")?.optString("href").orEmpty()
                )
            )
        }
        list
    }

    suspend fun searchTeamProfile(teamQuery: String, sport: SportType): TeamProfile? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(teamQuery, "UTF-8")
        val json = fetchJson("https://www.thesportsdb.com/api/v1/json/3/searchteams.php?t=$encoded")
            ?: return@withContext null
        diagnostics = diagnostics.copy(sportsDbOk = true)
        val teams = json.optJSONArray("teams") ?: return@withContext null
        val targetSport = if (sport == SportType.FOOTBALL) "Soccer" else "Cricket"
        val teamObj = (0 until teams.length())
            .mapNotNull { teams.optJSONObject(it) }
            .firstOrNull { it.optString("strSport").equals(targetSport, ignoreCase = true) }
            ?: teams.optJSONObject(0)
            ?: return@withContext null

        val teamId = teamObj.optString("idTeam")
        val playersJson = fetchJson("https://www.thesportsdb.com/api/v1/json/3/lookup_all_players.php?id=$teamId")
        val squad = mutableListOf<PlayerLineupEntry>()
        val pArr = playersJson?.optJSONArray("player")
        if (pArr != null) {
            for (i in 0 until pArr.length().coerceAtMost(25)) {
                val p = pArr.optJSONObject(i) ?: continue
                squad.add(
                    PlayerLineupEntry(
                        id = p.optString("idPlayer"),
                        name = p.optString("strPlayer"),
                        number = p.optString("strNumber", "-"),
                        position = p.optString("strPosition", "Player"),
                        rating = 7.2f + ((i % 4) * 0.2f),
                        starter = i < 11
                    )
                )
            }
        }

        TeamProfile(
            id = teamId,
            sport = sport,
            name = teamObj.optString("strTeam", teamQuery),
            country = teamObj.optString("strCountry", "International"),
            foundedYear = teamObj.optString("intFormedYear", "-"),
            stadium = teamObj.optString("strStadium", "Home Ground"),
            coach = teamObj.optString("strManager", "Head Coach").ifBlank { "Head Coach" },
            logoUrl = teamObj.optString("strBadge", teamObj.optString("strTeamBadge", "")),
            biography = teamObj.optString("strDescriptionEN", "Official club & squad profile via TheSportsDB."),
            recentForm = teamObj.optString("strLeague", ""),
            recordSummary = "League: ${teamObj.optString("strLeague", "-")}",
            leaguePosition = teamObj.optString("strLeague", "-"),
            teamRating = 7.8f,
            achievements = listOfNotNull(
                teamObj.optString("strLeague").takeIf { it.isNotBlank() },
                teamObj.optString("strStadiumLocation").takeIf { it.isNotBlank() }
            ),
            squad = squad
        )
    }

    suspend fun searchPlayerProfile(playerQuery: String, sport: SportType): PlayerProfile? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(playerQuery, "UTF-8")
        val json = fetchJson("https://www.thesportsdb.com/api/v1/json/3/searchplayers.php?p=$encoded")
            ?: return@withContext null
        diagnostics = diagnostics.copy(sportsDbOk = true)
        val arr = json.optJSONArray("player") ?: return@withContext null
        val p = arr.optJSONObject(0) ?: return@withContext null

        val stats = linkedMapOf<String, String>()
        p.optString("strHeight").takeIf { it.isNotBlank() }?.let { stats["Height"] = it }
        p.optString("strWeight").takeIf { it.isNotBlank() }?.let { stats["Weight"] = it }
        p.optString("strSide").takeIf { it.isNotBlank() }?.let { stats["Preferred"] = it }
        p.optString("strWage").takeIf { it.isNotBlank() }?.let { stats["Contract"] = it }
        p.optString("strSigning").takeIf { it.isNotBlank() }?.let { stats["Fee"] = it }

        val career = linkedMapOf<String, String>()
        p.optString("strSport").takeIf { it.isNotBlank() }?.let { career["Sport"] = it }
        p.optString("strNationality").takeIf { it.isNotBlank() }?.let { career["Nationality"] = it }
        p.optString("strPosition").takeIf { it.isNotBlank() }?.let { career["Role"] = it }
        p.optString("strTeam").takeIf { it.isNotBlank() }?.let { career["Club"] = it }
        p.optString("strTeam2").takeIf { it.isNotBlank() }?.let { career["National Team"] = it }

        PlayerProfile(
            id = p.optString("idPlayer"),
            sport = sport,
            fullName = p.optString("strPlayer", playerQuery),
            photoUrl = p.optString("strCutout", p.optString("strThumb", "")),
            biography = p.optString("strDescriptionEN", "Professional athlete profile retrieved from TheSportsDB."),
            dateOfBirth = p.optString("dateBorn", "-"),
            nationality = p.optString("strNationality", "-"),
            roleOrPosition = p.optString("strPosition", "Athlete"),
            currentTeam = p.optString("strTeam", "-"),
            previousTeams = listOfNotNull(p.optString("strTeam2").takeIf { it.isNotBlank() }),
            jerseyNumber = p.optString("strNumber", "-").ifBlank { "-" },
            rating = 7.9f,
            recentForm = listOf(7.2f, 7.8f, 8.1f, 7.6f, 8.3f),
            seasonStats = stats,
            careerStats = career,
            awards = listOfNotNull(p.optString("strStatus").takeIf { it.isNotBlank() })
        )
    }

    suspend fun fetchLeagueLeaders(leagueCode: String): List<LeaderStatRow> = withContext(Dispatchers.IO) {
        val json = fetchJson("https://site.api.espn.com/apis/site/v2/sports/soccer/$leagueCode/leaders")
            ?: return@withContext emptyList()
        val categories = json.optJSONArray("leaders") ?: json.optJSONArray("categories") ?: return@withContext emptyList()
        val list = mutableListOf<LeaderStatRow>()
        for (c in 0 until categories.length()) {
            val catObj = categories.optJSONObject(c) ?: continue
            val catName = catObj.optString("displayName", catObj.optString("name", "Leaders"))
            val leaders = catObj.optJSONArray("leaders") ?: continue
            for (i in 0 until leaders.length().coerceAtMost(5)) {
                val l = leaders.optJSONObject(i) ?: continue
                val athlete = l.optJSONObject("athlete") ?: continue
                val team = l.optJSONObject("team")
                list.add(
                    LeaderStatRow(
                        rank = i + 1,
                        playerId = athlete.optString("id"),
                        playerName = athlete.optString("displayName", "Player"),
                        teamName = team?.optString("abbreviation") ?: team?.optString("displayName").orEmpty(),
                        category = catName,
                        value = l.optString("displayValue", "0")
                    )
                )
            }
        }
        list
    }

    suspend fun fetchIccRankingsFromApi(): List<IccRankingRow> = withContext(Dispatchers.IO) {
        val rows = mutableListOf<IccRankingRow>()
        // Query ESPN Cricket header teams or standings if available
        val json = fetchJson("https://site.api.espn.com/apis/v2/sports/cricket/8039/standings")
        if (json != null) {
            val entries = json.optJSONArray("children")?.optJSONObject(0)
                ?.optJSONObject("standings")?.optJSONArray("entries")
            if (entries != null) {
                for (i in 0 until entries.length()) {
                    val e = entries.optJSONObject(i) ?: continue
                    val team = e.optJSONObject("team") ?: continue
                    val pts = e.optJSONArray("stats")?.optJSONObject(0)?.optInt("value", 0) ?: 0
                    rows.add(
                        IccRankingRow(
                            rank = i + 1,
                            format = "International",
                            category = "Teams",
                            name = team.optString("displayName", "Team"),
                            team = team.optString("abbreviation", ""),
                            ratingPoints = pts
                        )
                    )
                }
            }
        }
        rows
    }

    private fun parseIsoDate(raw: String): Long {
        if (raw.isBlank()) return System.currentTimeMillis()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                sdf.parse(raw)?.time?.let { return it }
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private companion object {
        const val TAG = "SportsRepository"
    }
}
