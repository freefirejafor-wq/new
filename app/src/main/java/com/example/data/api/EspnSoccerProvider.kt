package com.example.data.api

import com.example.data.model.MatchStatus
import com.example.util.DateTimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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

    override suspend fun fetchLiveAndUpcomingMatches(): Result<List<ProviderMatchData>> = withContext(Dispatchers.IO) {
        val allMatches = mutableListOf<ProviderMatchData>()
        var successCount = 0
        var lastException: Exception? = null

        for (league in defaultLeagues) {
            try {
                val url = "https://site.api.espn.com/apis/site/v2/sports/soccer/$league/scoreboard"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Matchora/1.0 (Android; Sports Scoreboard Client)")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val bodyString = response.body?.string() ?: return@use
                    val matches = parseEspnScoreboard(bodyString)
                    allMatches.addAll(matches)
                    successCount++
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        if (successCount > 0 || allMatches.isNotEmpty()) {
            Result.success(allMatches)
        } else {
            Result.failure(lastException ?: IllegalStateException("Unable to fetch soccer fixtures from ESPN scoreboard"))
        }
    }

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
