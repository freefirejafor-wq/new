package com.example.data.api

import com.example.data.model.MatchStatus
import com.example.util.DateTimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiFootballProvider(
    private val apiKey: String,
    private val baseEndpoint: String = "https://v3.football.api-sports.io",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
) : SportsMatchProvider {

    override val providerId: String = "API-FOOTBALL"
    override val displayName: String = "API-Football (Licensed Feed)"

    override suspend fun fetchLiveAndUpcomingMatches(): Result<List<ProviderMatchData>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("API-Football requires an API key. Please set your API key in Admin API Sync settings or Secrets.")
            )
        }

        try {
            val url = if (baseEndpoint.endsWith("/")) "${baseEndpoint}fixtures?live=all" else "$baseEndpoint/fixtures?live=all"
            val request = Request.Builder()
                .url(url)
                .header("x-apisports-key", apiKey.trim())
                .header("User-Agent", "Matchora/1.0 (Android; Sports Sync Client)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("API-Football returned HTTP ${response.code}")
                    )
                }
                val bodyString = response.body?.string() ?: ""
                val matches = parseApiFootballResponse(bodyString)
                Result.success(matches)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseApiFootballResponse(jsonString: String): List<ProviderMatchData> {
        val result = mutableListOf<ProviderMatchData>()
        try {
            val root = JSONObject(jsonString)
            val responseArray = root.optJSONArray("response") ?: return emptyList()

            for (i in 0 until responseArray.length()) {
                val item = responseArray.optJSONObject(i) ?: continue
                val fixture = item.optJSONObject("fixture") ?: continue
                val fixtureId = fixture.optLong("id", 0L).toString()
                if (fixtureId == "0") continue

                val timestampSec = fixture.optLong("timestamp", 0L)
                val utcTimestamp = if (timestampSec > 0) timestampSec * 1000L else System.currentTimeMillis()

                val venueObj = fixture.optJSONObject("venue")
                val venue = venueObj?.optString("name")

                val statusObj = fixture.optJSONObject("status")
                val shortStatus = statusObj?.optString("short") ?: "NS"
                val elapsed = statusObj?.optInt("elapsed", 0) ?: 0

                val (matchStatus, clockText) = mapApiFootballStatus(shortStatus, elapsed)

                val league = item.optJSONObject("league")
                val leagueName = league?.optString("name") ?: "Football League"

                val teams = item.optJSONObject("teams")
                val homeTeamObj = teams?.optJSONObject("home")
                val awayTeamObj = teams?.optJSONObject("away")

                val homeName = homeTeamObj?.optString("name") ?: "Home Team"
                val homeLogo = homeTeamObj?.optString("logo")
                val awayName = awayTeamObj?.optString("name") ?: "Away Team"
                val awayLogo = awayTeamObj?.optString("logo")

                val goals = item.optJSONObject("goals")
                val scoreHome = if (goals?.has("home") == true && !goals.isNull("home")) goals.optInt("home") else null
                val scoreAway = if (goals?.has("away") == true && !goals.isNull("away")) goals.optInt("away") else null

                result.add(
                    ProviderMatchData(
                        id = fixtureId,
                        providerName = providerId,
                        sportId = "football",
                        sportName = "Football",
                        competition = "FOOTBALL | ${leagueName.uppercase()}",
                        homeTeam = homeName,
                        homeLogoUrl = homeLogo,
                        awayTeam = awayName,
                        awayLogoUrl = awayLogo,
                        scoreHome = scoreHome,
                        scoreAway = scoreAway,
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

    private fun mapApiFootballStatus(short: String, elapsed: Int): Pair<MatchStatus, String> {
        return when (short.uppercase()) {
            "1H", "2H", "ET", "P", "LIVE" -> {
                val clock = if (elapsed > 0) "$elapsed'" else "Live"
                Pair(MatchStatus.LIVE, clock)
            }
            "HT" -> Pair(MatchStatus.HALFTIME, "HT")
            "FT", "AET", "PEN" -> Pair(MatchStatus.FINISHED, "FT")
            "PST" -> Pair(MatchStatus.POSTPONED, "Postponed")
            "CANC", "ABD" -> Pair(MatchStatus.CANCELLED, "Cancelled")
            else -> Pair(MatchStatus.UPCOMING, "Upcoming")
        }
    }
}
