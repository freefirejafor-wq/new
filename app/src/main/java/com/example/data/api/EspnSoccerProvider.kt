package com.example.data.api

import com.example.data.model.MatchStatus
import com.example.util.DateTimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class EspnSoccerProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
) : SportsMatchProvider {

    override val providerId: String = "ESPN"
    override val displayName: String = "ESPN Public Soccer Scoreboard"

    // Major soccer leagues available on public scoreboard
    private val defaultLeagues = listOf(
        "eng.1",           // Premier League
        "esp.1",           // La Liga
        "uefa.champions",  // UEFA Champions League
        "ita.1",           // Serie A
        "ger.1",           // Bundesliga
        "fra.1"            // Ligue 1
    )

    private val scheduleCacheLock = Any()
    private var cachedUpcomingMatches: List<ProviderMatchData> = emptyList()
    private var lastScheduleAttemptAt = 0L
    private var lastScheduleFetchAt = 0L

    private data class ScoreboardFetch(
        val success: Boolean,
        val matches: List<ProviderMatchData> = emptyList(),
        val error: Exception? = null
    )

    override suspend fun fetchLiveAndUpcomingMatches(): Result<List<ProviderMatchData>> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val today = formatEspnDate(now)
        val todayResults = fetchScoreboards(defaultLeagues.map { it to today })
        val todayMatches = todayResults.filter { it.success }.flatMap { it.matches }

        var scheduleSucceeded = false
        if (shouldRefreshSchedule(now)) {
            val futureDates = (1..7).map { offset ->
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply {
                    timeInMillis = now
                    add(Calendar.DAY_OF_YEAR, offset)
                }
                formatEspnDate(calendar.timeInMillis)
            }
            val scheduleResults = fetchScoreboards(
                defaultLeagues.flatMap { league -> futureDates.map { date -> league to date } }
            )
            val successfulResults = scheduleResults.filter { it.success }
            scheduleSucceeded = successfulResults.isNotEmpty()
            if (scheduleSucceeded) {
                synchronized(scheduleCacheLock) {
                    cachedUpcomingMatches = successfulResults
                        .flatMap { it.matches }
                        .filter { it.matchStatus == MatchStatus.UPCOMING }
                        .distinctBy { it.id }
                    lastScheduleFetchAt = now
                }
            }
        }

        val cachedUpcoming = synchronized(scheduleCacheLock) {
            cachedUpcomingMatches.filter { it.utcTimestamp > now }
        }
        val allMatches = (todayMatches + cachedUpcoming).distinctBy { it.id }

        if (todayResults.any { it.success } || scheduleSucceeded) {
            Result.success(allMatches)
        } else {
            val error = todayResults.firstNotNullOfOrNull { it.error }
                ?: IllegalStateException("Unable to fetch soccer fixtures from ESPN scoreboard")
            Result.failure(error)
        }
    }

    private suspend fun fetchScoreboards(requests: List<Pair<String, String>>): List<ScoreboardFetch> =
        coroutineScope {
            val semaphore = Semaphore(6)
            requests.map { (league, date) ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        fetchScoreboard(league, date)
                    }
                }
            }.awaitAll()
        }

    private fun fetchScoreboard(league: String, date: String): ScoreboardFetch {
        return try {
            val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/$league/scoreboard?dates=$date"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Matchora/1.0 (Android; Sports Scoreboard Client)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    ScoreboardFetch(
                        success = false,
                        error = IllegalStateException("ESPN returned HTTP ${response.code} for $league")
                    )
                } else {
                    val bodyString = response.body?.string().orEmpty()
                    ScoreboardFetch(success = true, matches = parseEspnScoreboard(bodyString))
                }
            }
        } catch (e: Exception) {
            ScoreboardFetch(success = false, error = e)
        }
    }

    private fun shouldRefreshSchedule(now: Long): Boolean = synchronized(scheduleCacheLock) {
        val retryDelay = if (lastScheduleFetchAt == 0L) 15 * 60 * 1000L else 4 * 60 * 60 * 1000L
        if (now - lastScheduleAttemptAt < retryDelay) {
            false
        } else {
            lastScheduleAttemptAt = now
            true
        }
    }

    private fun formatEspnDate(timestamp: Long): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(timestamp))

    private fun parseEspnScoreboard(jsonString: String): List<ProviderMatchData> {
        val result = mutableListOf<ProviderMatchData>()
        try {
            val root = JSONObject(jsonString)
            val leagueObj = root.optJSONArray("leagues")?.optJSONObject(0)
            val fallbackCompetitionName = leagueObj?.optString("name") ?: "Football League"

            val events = root.optJSONArray("events") ?: return emptyList()
            for (i in 0 until events.length()) {
                val event = events.optJSONObject(i) ?: continue
                val eventId = event.optString("id")
                if (eventId.isBlank()) continue

                val dateIso = event.optString("date")
                val utcTimestamp = DateTimeHelper.parseUtcIsoToMillis(dateIso)

                val competitions = event.optJSONArray("competitions")
                val comp = competitions?.optJSONObject(0) ?: continue

                val venueObj = comp.optJSONObject("venue")
                val venue = venueObj?.optString("fullName")

                val competitors = comp.optJSONArray("competitors") ?: continue
                var homeTeam = ""
                var homeLogo: String? = null
                var homeScore: Int? = null

                var awayTeam = ""
                var awayLogo: String? = null
                var awayScore: Int? = null

                for (c in 0 until competitors.length()) {
                    val compObj = competitors.optJSONObject(c) ?: continue
                    val homeAway = compObj.optString("homeAway")
                    val teamObj = compObj.optJSONObject("team")
                    val name = teamObj?.optString("displayName")?.ifBlank {
                        teamObj.optString("name")
                    } ?: "Team"

                    val logo = teamObj?.optString("logo")?.ifBlank {
                        teamObj.optJSONArray("logos")?.optJSONObject(0)?.optString("href")
                    }

                    val scoreStr = compObj.optString("score")
                    val score = scoreStr.toIntOrNull()

                    if (homeAway.equals("home", ignoreCase = true)) {
                        homeTeam = name
                        homeLogo = logo
                        homeScore = score
                    } else {
                        awayTeam = name
                        awayLogo = logo
                        awayScore = score
                    }
                }

                if (homeTeam.isBlank() || awayTeam.isBlank()) continue

                val statusObj = event.optJSONObject("status")
                val typeObj = statusObj?.optJSONObject("type")
                val state = typeObj?.optString("state")?.lowercase() ?: "pre"
                val typeName = typeObj?.optString("name")?.uppercase() ?: ""
                val displayClock = statusObj?.optString("displayClock")?.ifBlank { null }
                val shortDetail = typeObj?.optString("shortDetail") ?: ""

                val (matchStatus, clockText) = mapEspnStatus(state, typeName, displayClock, shortDetail)

                val competition = fallbackCompetitionName.uppercase()

                result.add(
                    ProviderMatchData(
                        id = eventId,
                        providerName = providerId,
                        sportId = "football",
                        sportName = "Football",
                        competition = "FOOTBALL | $competition",
                        homeTeam = homeTeam,
                        homeLogoUrl = homeLogo,
                        awayTeam = awayTeam,
                        awayLogoUrl = awayLogo,
                        scoreHome = homeScore,
                        scoreAway = awayScore,
                        matchStatus = matchStatus,
                        clockText = clockText,
                        utcTimestamp = utcTimestamp,
                        venue = venue
                    )
                )
            }
        } catch (_: Exception) { }
        return result
    }

    private fun mapEspnStatus(
        state: String,
        typeName: String,
        displayClock: String?,
        shortDetail: String
    ): Pair<MatchStatus, String> {
        when {
            typeName.contains("POSTPONED") -> {
                return Pair(MatchStatus.POSTPONED, "Postponed")
            }
            typeName.contains("CANCEL") -> {
                return Pair(MatchStatus.CANCELLED, "Cancelled")
            }
            typeName.contains("HALFTIME") || typeName.contains("HALF_TIME") || shortDetail.contains("HT") -> {
                return Pair(MatchStatus.HALFTIME, "HT")
            }
            state == "in" || typeName.contains("IN_PROGRESS") || typeName.contains("FIRST_HALF") || typeName.contains("SECOND_HALF") -> {
                val clock = displayClock ?: shortDetail.ifBlank { "Live" }
                return Pair(MatchStatus.LIVE, clock)
            }
            state == "post" || typeName.contains("FINAL") || typeName.contains("FULL_TIME") -> {
                return Pair(MatchStatus.FINISHED, "FT")
            }
            else -> {
                return Pair(MatchStatus.UPCOMING, shortDetail.ifBlank { "Upcoming" })
            }
        }
    }
}
