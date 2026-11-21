package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.IccRankingRow
import com.example.data.model.LeaderStatRow
import com.example.data.model.LeagueStandingRow
import com.example.data.model.MatchStatus
import com.example.data.model.MatchSummary
import com.example.data.model.SportType
import com.example.data.model.SportsNewsItem
import com.example.data.model.UiState
import com.example.i18n.AppStrings
import com.example.ui.components.MatchSummaryCard
import com.example.ui.components.SportSwitcherPill
import com.example.ui.components.TeamBadgeImage
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.viewmodel.SportsViewModel

private enum class ScoresSubTab { MATCHES, TABLE, LEADERS, BRACKETS, NEWS }

@Composable
fun MatchesDashboardScreen(
    sportsVm: SportsViewModel,
    strings: AppStrings,
    selectedSport: SportType,
    selectedLeague: String,
    statusFilter: MatchStatus?,
    matchesState: UiState<List<MatchSummary>>,
    standingsState: UiState<List<LeagueStandingRow>>,
    iccRankings: List<IccRankingRow>,
    leaders: List<LeaderStatRow>,
    newsState: UiState<List<SportsNewsItem>>,
    onOpenMatch: (MatchSummary) -> Unit,
    onOpenTeam: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onBookmarkNews: (SportsNewsItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableStateOf(ScoresSubTab.MATCHES) }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Header + Sport Switcher
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = strings.apiStatusBanner,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = { sportsVm.refreshAllForCurrentSport() },
                    modifier = Modifier.testTag("refresh_scores_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = strings.refreshLive,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            SportSwitcherPill(
                selectedSport = selectedSport,
                footballLabel = strings.football,
                cricketLabel = strings.cricket,
                onSelectSport = { sportsVm.selectSport(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Quick Search (Team / Player / Match)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search match, team or player (e.g. Arsenal, Kohli)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        Row {
                            FilterChip(
                                selected = false,
                                onClick = { onOpenTeam(searchQuery.trim()) },
                                label = { Text("Team") },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = false,
                                onClick = { onOpenPlayer(searchQuery.trim()) },
                                label = { Text("Player") },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input")
            )

            // Sub-section pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    ScoresSubTab.MATCHES to strings.navScores,
                    ScoresSubTab.TABLE to if (selectedSport == SportType.FOOTBALL) strings.standings else strings.rankings,
                    ScoresSubTab.LEADERS to "Leaders",
                    ScoresSubTab.BRACKETS to "Brackets",
                    ScoresSubTab.NEWS to strings.news
                )
                tabs.forEach { (tab, label) ->
                    FilterChip(
                        selected = subTab == tab,
                        onClick = { subTab = tab },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("subtab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (subTab) {
            ScoresSubTab.MATCHES -> {
                MatchesFeedSection(
                    sportsVm = sportsVm,
                    strings = strings,
                    selectedSport = selectedSport,
                    selectedLeague = selectedLeague,
                    statusFilter = statusFilter,
                    searchQuery = searchQuery,
                    matchesState = matchesState,
                    onOpenMatch = onOpenMatch
                )
            }
            ScoresSubTab.TABLE -> {
                StandingsOrRankingsSection(
                    selectedSport = selectedSport,
                    strings = strings,
                    standingsState = standingsState,
                    iccRankings = iccRankings,
                    onOpenTeam = onOpenTeam
                )
            }
            ScoresSubTab.LEADERS -> {
                LeadersSection(
                    leaders = leaders,
                    selectedSport = selectedSport,
                    onOpenPlayer = onOpenPlayer
                )
            }
            ScoresSubTab.BRACKETS -> {
                TournamentBracketSection(
                    selectedSport = selectedSport,
                    matchesState = matchesState,
                    onOpenMatch = onOpenMatch
                )
            }
            ScoresSubTab.NEWS -> {
                NewsSection(
                    newsState = newsState,
                    strings = strings,
                    onBookmarkNews = onBookmarkNews
                )
            }
        }
    }
}

@Composable
private fun MatchesFeedSection(
    sportsVm: SportsViewModel,
    strings: AppStrings,
    selectedSport: SportType,
    selectedLeague: String,
    statusFilter: MatchStatus?,
    searchQuery: String,
    matchesState: UiState<List<MatchSummary>>,
    onOpenMatch: (MatchSummary) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedSport == SportType.FOOTBALL) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedLeague == "ALL",
                        onClick = { sportsVm.selectLeague("ALL") },
                        label = { Text("All Leagues") }
                    )
                    sportsVm.leagues.forEach { league ->
                        FilterChip(
                            selected = selectedLeague == league.code,
                            onClick = { sportsVm.selectLeague(league.code) },
                            label = { Text(league.name) }
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { sportsVm.setStatusFilter(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = statusFilter == MatchStatus.LIVE,
                    onClick = { sportsVm.setStatusFilter(MatchStatus.LIVE) },
                    label = { Text(strings.live) }
                )
                FilterChip(
                    selected = statusFilter == MatchStatus.UPCOMING,
                    onClick = { sportsVm.setStatusFilter(MatchStatus.UPCOMING) },
                    label = { Text(strings.upcoming) }
                )
                FilterChip(
                    selected = statusFilter == MatchStatus.FINISHED,
                    onClick = { sportsVm.setStatusFilter(MatchStatus.FINISHED) },
                    label = { Text(strings.results) }
                )
            }
        }

        when (matchesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(10.dp))
                            Text(strings.loadingLiveFeeds, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            is UiState.Error -> {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(matchesState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            is UiState.Success -> {
                val q = searchQuery.trim().lowercase()
                val filtered = matchesState.data.filter { match ->
                    (statusFilter == null || match.status == statusFilter) &&
                        (q.isBlank() ||
                            match.home.name.lowercase().contains(q) ||
                            match.away.name.lowercase().contains(q) ||
                            match.leagueName.lowercase().contains(q))
                }
                if (filtered.isEmpty()) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = strings.noLiveMatches,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (statusFilter != null) {
                                    FilterChip(
                                        selected = false,
                                        onClick = { sportsVm.setStatusFilter(null) },
                                        label = { Text("Show All Real Fixtures & Results") }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { match ->
                        MatchSummaryCard(
                            match = match,
                            liveLabel = strings.live,
                            onClick = { onOpenMatch(match) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsOrRankingsSection(
    selectedSport: SportType,
    strings: AppStrings,
    standingsState: UiState<List<LeagueStandingRow>>,
    iccRankings: List<IccRankingRow>,
    onOpenTeam: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selectedSport == SportType.FOOTBALL) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("#", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(28.dp))
                        Text("Club", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        Text("P", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(30.dp))
                        Text("W", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(30.dp))
                        Text("GD", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
                        Text("PTS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
                    }
                }
            }
            when (standingsState) {
                is UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item { Text(standingsState.message, color = MaterialTheme.colorScheme.error) }
                is UiState.Success -> {
                    items(standingsState.data, key = { "${it.rank}_${it.teamId}" }) { row ->
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            onClick = { onOpenTeam(row.teamName) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${row.rank}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (row.rank <= 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(28.dp)
                                )
                                TeamBadgeImage(row.teamLogo, row.teamName.take(3), size = 24.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = row.teamName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("${row.played}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
                                Text("${row.won}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
                                Text(row.goalDiff, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
                                Text(
                                    "${row.points}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Text(strings.rankings, style = MaterialTheme.typography.titleLarge)
            }
            if (iccRankings.isEmpty()) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Tap Refresh or search any international Cricket team (India, Bangladesh, Australia, England, Pakistan) above to inspect live squad, ratings, and stats.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                items(iccRankings) { row ->
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenTeam(row.name) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${row.rank}  ${row.name}", style = MaterialTheme.typography.titleMedium)
                            Text("${row.ratingPoints} pts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeadersSection(
    leaders: List<LeaderStatRow>,
    selectedSport: SportType,
    onOpenPlayer: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (leaders.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (selectedSport == SportType.CRICKET) {
                                "Open any Cricket match to view live batting strike rates, bowling economy, powerplay stats, and player ratings, or search any cricketer above."
                            } else {
                                "Select a Football league (Premier League, La Liga, UCL) to load real top scorers, assists, and discipline leaders."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(leaders) { leader ->
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenPlayer(leader.playerName) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(leader.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text("#${leader.rank} ${leader.playerName}", style = MaterialTheme.typography.titleMedium)
                            if (leader.teamName.isNotBlank()) {
                                Text(leader.teamName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(leader.value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentBracketSection(
    selectedSport: SportType,
    matchesState: UiState<List<MatchSummary>>,
    onOpenMatch: (MatchSummary) -> Unit
) {
    val matches = (matchesState as? UiState.Success)?.data.orEmpty().take(8)
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (selectedSport == SportType.FOOTBALL) "Knockout & Matchday Bracket" else "Series & Tournament Progression",
                style = MaterialTheme.typography.titleLarge
            )
        }
        if (matches.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No active fixtures loaded yet for bracket view.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(matches.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { match ->
                        LiquidGlassCard(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(12.dp),
                            onClick = { onOpenMatch(match) }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(match.leagueName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${match.home.shortName}  ${match.home.score.ifBlank { "-" }}", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text("${match.away.shortName}  ${match.away.score.ifBlank { "-" }}", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(match.clockDisplay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsSection(
    newsState: UiState<List<SportsNewsItem>>,
    strings: AppStrings,
    onBookmarkNews: (SportsNewsItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (newsState) {
            is UiState.Loading -> item { CircularProgressIndicator() }
            is UiState.Error -> item { Text(newsState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                items(newsState.data, key = { it.id }) { article ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (article.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = article.imageUrl,
                                    contentDescription = article.headline,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = article.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { onBookmarkNews(article) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.BookmarkAdd,
                                        contentDescription = strings.save,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(article.headline, style = MaterialTheme.typography.titleLarge)
                            if (article.description.isNotBlank()) {
                                Text(
                                    text = article.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
