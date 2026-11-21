package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MatchDetail
import com.example.data.model.SportType
import com.example.data.model.UiState
import com.example.i18n.AppStrings
import com.example.ui.components.CricketOverChart
import com.example.ui.components.FootballPitchCanvas
import com.example.ui.components.PlayerHeatmapCanvas
import com.example.ui.components.StatComparisonBar
import com.example.ui.components.TeamBadgeImage
import com.example.ui.components.addMatchToCalendar
import com.example.ui.components.shareTextCard
import com.example.ui.theme.LiquidGlassCard

private enum class DetailSection { OVERVIEW, STATS_OR_SCORECARD, LINEUPS, COMMENTARY, AI_INSIGHT }

@Composable
fun MatchDetailScreen(
    detailState: UiState<MatchDetail>,
    aiInsightState: UiState<String>?,
    strings: AppStrings,
    onBack: () -> Unit,
    onRunAiAnalysis: (MatchDetail) -> Unit,
    onOpenTeam: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onSaveOffline: (MatchDetail) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(DetailSection.OVERVIEW) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("match_detail_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Match Center",
                style = MaterialTheme.typography.titleLarge
            )
            if (detailState is UiState.Success) {
                val summary = detailState.data.summary
                Row {
                    IconButton(onClick = { addMatchToCalendar(context, summary) }) {
                        Icon(Icons.Filled.Event, contentDescription = strings.addReminder, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { onSaveOffline(detailState.data) }) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = strings.saveOffline, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        shareTextCard(
                            context,
                            "${summary.home.name} vs ${summary.away.name}",
                            "Score: ${summary.home.score} - ${summary.away.score}\n${summary.leagueName} • ${summary.clockDisplay}"
                        )
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = strings.shareCard, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }

        when (detailState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is UiState.Error -> {
                LiquidGlassCard(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(detailState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val detail = detailState.data
                val summary = detail.summary
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Hero Scoreboard Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${summary.leagueName} • ${summary.clockDisplay}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        TeamBadgeImage(summary.home.logoUrl, summary.home.shortName, size = 48.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = summary.home.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        FilterChip(
                                            selected = false,
                                            onClick = { onOpenTeam(summary.home.name) },
                                            label = { Text("Club") }
                                        )
                                    }

                                    Text(
                                        text = if (summary.sport == SportType.FOOTBALL) {
                                            "${summary.home.score.ifBlank { "0" }} : ${summary.away.score.ifBlank { "0" }}"
                                        } else {
                                            "VS"
                                        },
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        TeamBadgeImage(summary.away.logoUrl, summary.away.shortName, size = 48.dp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = summary.away.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        FilterChip(
                                            selected = false,
                                            onClick = { onOpenTeam(summary.away.name) },
                                            label = { Text("Club") }
                                        )
                                    }
                                }
                                if (summary.sport == SportType.CRICKET) {
                                    Text(
                                        text = "${summary.home.shortName}: ${summary.home.score.ifBlank { "-" }}  |  ${summary.away.shortName}: ${summary.away.score.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Stadium, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = listOf(summary.venue, detail.stadiumCapacity, detail.tossInfo).filter { it.isNotBlank() }.joinToString(" • "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Section Selector
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tabLabels = listOf(
                                DetailSection.OVERVIEW to strings.timeline,
                                DetailSection.STATS_OR_SCORECARD to if (summary.sport == SportType.FOOTBALL) strings.stats else strings.scorecard,
                                DetailSection.LINEUPS to strings.lineups,
                                DetailSection.COMMENTARY to strings.commentary,
                                DetailSection.AI_INSIGHT to strings.aiMatchAnalysis
                            )
                            tabLabels.forEach { (sec, label) ->
                                FilterChip(
                                    selected = activeTab == sec,
                                    onClick = {
                                        activeTab = sec
                                        if (sec == DetailSection.AI_INSIGHT && aiInsightState == null) {
                                            onRunAiAnalysis(detail)
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    when (activeTab) {
                        DetailSection.OVERVIEW -> {
                            if (detail.headToHeadSummary.isNotBlank()) {
                                item {
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column {
                                            Text(strings.headToHead, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.height(4.dp))
                                            Text(detail.headToHeadSummary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                            if (detail.timeline.isEmpty()) {
                                item {
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = detail.summary.statusNote.ifBlank { "Match events and timeline will populate live as play occurs." },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            } else {
                                items(detail.timeline) { ev ->
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = ev.minute.ifBlank { "•" },
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(46.dp)
                                            )
                                            Column {
                                                Text(ev.title, style = MaterialTheme.typography.titleMedium)
                                                Text(ev.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DetailSection.STATS_OR_SCORECARD -> {
                            if (summary.sport == SportType.FOOTBALL) {
                                if (detail.stats.isEmpty()) {
                                    item {
                                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Text("Live match statistics (possession, shots, xG, corners) are not yet published for this fixture.", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                } else {
                                    item {
                                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                                detail.stats.forEach { st ->
                                                    StatComparisonBar(st.label, st.homeValue, st.awayValue, st.homeRatio)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Cricket Scorecards + Over Progression Chart
                                if (detail.cricketInnings.isEmpty()) {
                                    item {
                                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Text("Detailed innings scorecard will appear once batting/bowling data is published by the live feed.", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                } else {
                                    detail.cricketInnings.forEach { inn ->
                                        item {
                                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(inn.title, style = MaterialTheme.typography.titleLarge)
                                                        Text(inn.totalScore, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    if (inn.runRate.isNotBlank() || inn.requiredRunRate.isNotBlank()) {
                                                        Text(
                                                            "${inn.runRate}  ${inn.requiredRunRate}".trim(),
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                    if (inn.overProgression.isNotEmpty()) {
                                                        Text("${strings.overs} Run Progression", style = MaterialTheme.typography.labelMedium)
                                                        CricketOverChart(inn.overProgression)
                                                    }
                                                    if (inn.batters.isNotEmpty()) {
                                                        Text(strings.batting, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                                        inn.batters.forEach { b ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(b.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                                                    Text(b.dismissal, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                }
                                                                Text("${b.runs}(${b.balls}) • SR ${b.strikeRate}", style = MaterialTheme.typography.labelMedium)
                                                            }
                                                        }
                                                    }
                                                    if (inn.bowlers.isNotEmpty()) {
                                                        Text(strings.bowling, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                                        inn.bowlers.forEach { bw ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(bw.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                                                Text("${bw.overs}-${bw.maidens}-${bw.runs}-${bw.wickets} (Ec ${bw.economy})", style = MaterialTheme.typography.labelMedium)
                                                            }
                                                        }
                                                    }
                                                    if (inn.fallOfWickets.isNotEmpty()) {
                                                        Text(strings.fallOfWickets, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                                        Text(
                                                            inn.fallOfWickets.joinToString(" • ") { "${it.wicketNumber}-${it.score} (${it.batterName}, ${it.over} ov)" },
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DetailSection.LINEUPS -> {
                            if (summary.sport == SportType.FOOTBALL && (detail.homeLineup.isNotEmpty() || detail.awayLineup.isNotEmpty())) {
                                item {
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                "Formations: ${detail.homeFormation.ifBlank { "XI" }} vs ${detail.awayFormation.ifBlank { "XI" }}",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            FootballPitchCanvas(detail.homeLineup, detail.awayLineup)
                                        }
                                    }
                                }
                            }
                            val combined = detail.homeLineup.map { summary.home.shortName to it } +
                                detail.awayLineup.map { summary.away.shortName to it }
                            if (combined.isEmpty()) {
                                item {
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Confirmed Starting XI / Playing XI not yet released.", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                items(combined) { (teamCode, player) ->
                                    LiquidGlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(12.dp),
                                        onClick = { onOpenPlayer(player.name) }
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "#${player.number} ${player.name} ($teamCode • ${player.position})",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = String.format("%.1f", player.rating),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            if (player.heatZones.isNotEmpty()) {
                                                PlayerHeatmapCanvas(player.heatZones)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        DetailSection.COMMENTARY -> {
                            if (detail.commentary.isEmpty()) {
                                item {
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("Live commentary entries are not available for this fixture.", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                items(detail.commentary) { line ->
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                                        Text(line, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        DetailSection.AI_INSIGHT -> {
                            item {
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(8.dp))
                                            Text(strings.aiMatchAnalysis, style = MaterialTheme.typography.titleLarge)
                                        }
                                        when (aiInsightState) {
                                            null, is UiState.Loading -> {
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                                Text("Generating Gemini tactical breakdown...", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            is UiState.Error -> {
                                                Text(aiInsightState.message, color = MaterialTheme.colorScheme.error)
                                                Button(onClick = { onRunAiAnalysis(detail) }) { Text("Retry Analysis") }
                                            }
                                            is UiState.Success -> {
                                                Text(aiInsightState.data, style = MaterialTheme.typography.bodyLarge)
                                                Button(
                                                    onClick = { onRunAiAnalysis(detail) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                                ) {
                                                    Text("Regenerate")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
