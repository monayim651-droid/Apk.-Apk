package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PlayerProfile
import com.example.data.model.TeamProfile
import com.example.data.model.UiState
import com.example.i18n.AppStrings
import com.example.ui.components.PlayerHeatmapCanvas
import com.example.ui.components.StatComparisonBar
import com.example.ui.components.TeamBadgeImage
import com.example.ui.theme.LiquidGlassCard

@Composable
fun TeamProfileScreen(
    teamState: UiState<TeamProfile>,
    strings: AppStrings,
    isFavourite: Boolean,
    onToggleFavourite: (String) -> Unit,
    onOpenPlayer: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Team Profile", style = MaterialTheme.typography.titleLarge)
            if (teamState is UiState.Success) {
                IconButton(onClick = { onToggleFavourite(teamState.data.name) }) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favourite Team",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else Spacer(Modifier.width(48.dp))
        }

        when (teamState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> LiquidGlassCard(Modifier.padding(16.dp).fillMaxWidth()) { Text(teamState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                val team = teamState.data
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                TeamBadgeImage(team.logoUrl, team.name.take(3), size = 72.dp)
                                Spacer(Modifier.height(10.dp))
                                Text(team.name, style = MaterialTheme.typography.headlineMedium)
                                Text("${team.country} • Founded ${team.foundedYear}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("${strings.stadium}: ${team.stadium} | ${strings.manager}: ${team.coach}", style = MaterialTheme.typography.bodyMedium)
                                Text("Rating: ${team.teamRating} • ${team.recordSummary}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Club Biography", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text(team.biography.take(650), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    item {
                        Text(strings.squad, style = MaterialTheme.typography.titleLarge)
                    }
                    items(team.squad) { player ->
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp),
                            onClick = { onOpenPlayer(player.name) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("#${player.number} ${player.name}", style = MaterialTheme.typography.titleMedium)
                                    Text(player.position, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(String.format("%.1f", player.rating), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerProfileScreen(
    playerState: UiState<PlayerProfile>,
    aiInsightState: UiState<String>?,
    strings: AppStrings,
    isFavourite: Boolean,
    onToggleFavourite: (String) -> Unit,
    onRunAiAnalysis: (PlayerProfile) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Player Profile", style = MaterialTheme.typography.titleLarge)
            if (playerState is UiState.Success) {
                IconButton(onClick = { onToggleFavourite(playerState.data.fullName) }) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favourite Player",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else Spacer(Modifier.width(48.dp))
        }

        when (playerState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Error -> LiquidGlassCard(Modifier.padding(16.dp).fillMaxWidth()) { Text(playerState.message, color = MaterialTheme.colorScheme.error) }
            is UiState.Success -> {
                val p = playerState.data
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                if (p.photoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = p.photoUrl,
                                        contentDescription = p.fullName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(90.dp).clip(CircleShape)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("#${p.jerseyNumber} ${p.fullName}", style = MaterialTheme.typography.headlineMedium)
                                Text("${p.currentTeam} • ${p.roleOrPosition}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                Text("${p.nationality} • Born ${p.dateOfBirth}", style = MaterialTheme.typography.bodyMedium)
                                Text("Rating: ${p.rating}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(strings.form, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                PlayerHeatmapCanvas(p.recentForm.map { (it / 10f).coerceIn(0.3f, 1f) })
                                (p.careerStats + p.seasonStats).forEach { (k, v) ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(v, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.aiPlayerAnalysis, style = MaterialTheme.typography.titleLarge)
                                }
                                when (aiInsightState) {
                                    null -> Button(
                                        onClick = { onRunAiAnalysis(p) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    ) {
                                        Text("Generate AI Scouting Report")
                                    }
                                    is UiState.Loading -> CircularProgressIndicator()
                                    is UiState.Error -> Text(aiInsightState.message, color = MaterialTheme.colorScheme.error)
                                    is UiState.Success -> Text(aiInsightState.data, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(p.biography.take(800), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonToolScreen(
    teamA: TeamProfile?,
    teamB: TeamProfile?,
    playerA: PlayerProfile?,
    playerB: PlayerProfile?,
    strings: AppStrings,
    onCompareTeams: (String, String) -> Unit,
    onComparePlayers: (String, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var teamInputA by remember { mutableStateOf("Arsenal") }
    var teamInputB by remember { mutableStateOf("Real Madrid") }
    var playerInputA by remember { mutableStateOf("Bukayo Saka") }
    var playerInputB by remember { mutableStateOf("Virat Kohli") }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("${strings.compareTeams} & ${strings.comparePlayers}", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.compareTeams, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = teamInputA, onValueChange = { teamInputA = it }, label = { Text("Team A") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = teamInputB, onValueChange = { teamInputB = it }, label = { Text("Team B") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Button(onClick = { onCompareTeams(teamInputA, teamInputB) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Compare Real Clubs")
                        }
                        if (teamA != null && teamB != null) {
                            StatComparisonBar("Founded", teamA.foundedYear, teamB.foundedYear, 0.5f)
                            StatComparisonBar("Squad Size", "${teamA.squad.size}", "${teamB.squad.size}", 0.5f)
                            StatComparisonBar("Rating", "${teamA.teamRating}", "${teamB.teamRating}", 0.52f)
                        }
                    }
                }
            }
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.comparePlayers, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = playerInputA, onValueChange = { playerInputA = it }, label = { Text("Player A") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = playerInputB, onValueChange = { playerInputB = it }, label = { Text("Player B") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Button(onClick = { onComparePlayers(playerInputA, playerInputB) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Compare Real Athletes")
                        }
                        if (playerA != null && playerB != null) {
                            StatComparisonBar("Rating", "${playerA.rating}", "${playerB.rating}", 0.5f)
                            StatComparisonBar("Team", playerA.currentTeam, playerB.currentTeam, 0.5f)
                            StatComparisonBar("Role", playerA.roleOrPosition, playerB.roleOrPosition, 0.5f)
                        }
                    }
                }
            }
        }
    }
}
