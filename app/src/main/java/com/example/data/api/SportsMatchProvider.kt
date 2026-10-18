package com.example.data.api

import com.example.data.model.MatchStatus

data class ProviderMatchData(
    val id: String, // Stable provider event ID
    val providerName: String,
    val sportId: String = "football",
    val sportName: String = "Football",
    val competition: String,
    val homeTeam: String,
    val homeLogoUrl: String? = null,
    val awayTeam: String,
    val awayLogoUrl: String? = null,
    val scoreHome: Int? = null,
    val scoreAway: Int? = null,
    val matchStatus: MatchStatus,
    val clockText: String,
    val utcTimestamp: Long,
    val venue: String? = null
)

interface SportsMatchProvider {
    val providerId: String
    val displayName: String
    suspend fun fetchLiveAndUpcomingMatches(): Result<List<ProviderMatchData>>
}
