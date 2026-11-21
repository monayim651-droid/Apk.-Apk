package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BookmarkItem
import com.example.data.model.MatchSummary
import com.example.data.model.SportType
import com.example.data.model.TriviaScore
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.UserNotification
import com.example.data.repository.TriviaQuestion
import com.example.i18n.AppStrings
import com.example.ui.components.MatchSummaryCard
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.viewmodel.SocialHubViewModel

private enum class ArenaTab { TRIVIA, LEADERBOARD, BOOKMARKS, COMPARE, ALERTS }

@Composable
fun ExploreAndFeaturesScreen(
    vm: SocialHubViewModel,
    strings: AppStrings,
    userAccount: UserAccount?,
    triviaState: UiState<List<TriviaQuestion>>?,
    leaderboardState: UiState<List<TriviaScore>>,
    bookmarksState: UiState<List<BookmarkItem>>,
    notificationsState: UiState<List<UserNotification>>,
    featuredLiveMatch: MatchSummary?,
    onOpenComparisonTool: () -> Unit,
    onOpenMatch: (MatchSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ArenaTab.TRIVIA) }
    var triviaSport by remember { mutableStateOf(SportType.FOOTBALL) }
    var qIndex by remember { mutableIntStateOf(0) }
    var selectedOpt by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(strings.navArena, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ArenaTab.TRIVIA to strings.sportsQuiz,
                    ArenaTab.LEADERBOARD to strings.leaderboard,
                    ArenaTab.BOOKMARKS to strings.bookmarks,
                    ArenaTab.COMPARE to strings.compareTeams,
                    ArenaTab.ALERTS to "Alerts"
                ).forEach { (tab, label) ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text(label) },
                        modifier = Modifier.testTag("arena_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (featuredLiveMatch != null) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Live Match Widget Preview", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            MatchSummaryCard(
                                match = featuredLiveMatch,
                                liveLabel = strings.live,
                                onClick = { onOpenMatch(featuredLiveMatch) }
                            )
                        }
                    }
                }
            }

            when (activeTab) {
                ArenaTab.TRIVIA -> {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${strings.sportsQuiz} (Gemini AI)", style = MaterialTheme.typography.titleLarge)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = triviaSport == SportType.FOOTBALL, onClick = { triviaSport = SportType.FOOTBALL }, label = { Text("Football") })
                                    FilterChip(selected = triviaSport == SportType.CRICKET, onClick = { triviaSport = SportType.CRICKET }, label = { Text("Cricket") })
                                }
                                Button(
                                    onClick = {
                                        qIndex = 0
                                        selectedOpt = null
                                        vm.loadDailyTrivia(triviaSport)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    modifier = Modifier.fillMaxWidth().testTag("start_ai_trivia_button")
                                ) {
                                    Text("Generate Daily AI Trivia")
                                }

                                when (triviaState) {
                                    null -> Text("Tap above to generate fresh Football or Cricket trivia questions and climb the Fan Leaderboard.", style = MaterialTheme.typography.bodyMedium)
                                    is UiState.Loading -> CircularProgressIndicator()
                                    is UiState.Error -> Text(triviaState.message, color = MaterialTheme.colorScheme.error)
                                    is UiState.Success -> {
                                        val qs = triviaState.data
                                        val q = qs.getOrNull(qIndex)
                                        if (q != null) {
                                            Text("Q${qIndex + 1}/${qs.size}: ${q.question}", style = MaterialTheme.typography.titleMedium)
                                            q.options.forEachIndexed { idx, opt ->
                                                val chosen = selectedOpt == idx
                                                OutlinedButton(
                                                    onClick = {
                                                        if (selectedOpt == null) {
                                                            selectedOpt = idx
                                                            if (idx == q.correctIndex) {
                                                                vm.submitTriviaScore(20L, 1L)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = if (chosen) {
                                                            if (idx == q.correctIndex) "✓ $opt" else "✗ $opt"
                                                        } else opt
                                                    )
                                                }
                                            }
                                            if (selectedOpt != null) {
                                                Text(q.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                                if (qIndex < qs.lastIndex) {
                                                    Button(onClick = { qIndex++; selectedOpt = null }) { Text("Next Question") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.achievements, style = MaterialTheme.typography.titleLarge)
                                }
                                Text("Points: ${userAccount?.triviaPoints ?: 0} • Streak: ${userAccount?.triviaStreak ?: 0}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                val badges = userAccount?.achievements.orEmpty().ifEmpty { listOf("FIRST_SIGN_IN") }
                                Text("Unlocked Badges: ${badges.joinToString(" • ")}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                ArenaTab.LEADERBOARD -> {
                    when (leaderboardState) {
                        is UiState.Loading -> item { CircularProgressIndicator() }
                        is UiState.Error -> item { Text(leaderboardState.message, color = MaterialTheme.colorScheme.error) }
                        is UiState.Success -> {
                            items(leaderboardState.data) { row ->
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(row.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("${row.badge} • Streak ${row.streak}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("${row.points} pts", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                ArenaTab.BOOKMARKS -> {
                    when (bookmarksState) {
                        is UiState.Loading -> item { CircularProgressIndicator() }
                        is UiState.Error -> item { Text(bookmarksState.message, color = MaterialTheme.colorScheme.error) }
                        is UiState.Success -> {
                            if (bookmarksState.data.isEmpty()) {
                                item {
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("No saved matches, news, or offline stats yet. Tap the bookmark icon on any match or article.", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                items(bookmarksState.data, key = { it.id }) { bm ->
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${bm.bookmarkType} • ${bm.sport}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                Text(bm.title, style = MaterialTheme.typography.titleMedium)
                                                Text(bm.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(onClick = { vm.removeBookmark(bm.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Remove bookmark")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ArenaTab.COMPARE -> {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${strings.compareTeams} & ${strings.comparePlayers}", style = MaterialTheme.typography.titleLarge)
                                }
                                Text("Compare real Football clubs, Cricket nations, and athletes side-by-side from TheSportsDB.", style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = onOpenComparisonTool, modifier = Modifier.fillMaxWidth()) {
                                    Text("Open Comparison Studio")
                                }
                            }
                        }
                    }
                }

                ArenaTab.ALERTS -> {
                    val alerts = (notificationsState as? UiState.Success)?.data.orEmpty()
                    if (alerts.isEmpty()) {
                        item {
                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Text("All caught up! Match alerts, social notifications, and app updates appear here.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        items(alerts, key = { it.id }) { alert ->
                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text("${alert.category} • ${alert.title}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(alert.body, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
