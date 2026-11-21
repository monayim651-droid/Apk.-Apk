package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CricketOverPoint
import com.example.data.model.MatchStatus
import com.example.data.model.MatchSummary
import com.example.data.model.PlayerLineupEntry
import com.example.data.model.SportType
import com.example.i18n.AppStrings
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalMontanuColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SportSwitcherPill(
    selectedSport: SportType,
    footballLabel: String,
    cricketLabel: String,
    onSelectSport: (SportType) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMontanuColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
            .border(1.dp, ext.glassBorderTop, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SportTabButton(
            selected = selectedSport == SportType.FOOTBALL,
            label = footballLabel,
            icon = Icons.Filled.SportsSoccer,
            testTag = "switch_football",
            onClick = { onSelectSport(SportType.FOOTBALL) },
            modifier = Modifier.weight(1f)
        )
        SportTabButton(
            selected = selectedSport == SportType.CRICKET,
            label = cricketLabel,
            icon = Icons.Filled.SportsCricket,
            testTag = "switch_cricket",
            onClick = { onSelectSport(SportType.CRICKET) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SportTabButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(),
        label = "sportBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(),
        label = "sportText"
    )
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

enum class MainNavTab(val route: String) {
    SCORES("scores"),
    SOCIAL("social"),
    ARENA("arena"),
    HUB("hub")
}

@Composable
fun FloatingGlassNavBar(
    currentTab: MainNavTab,
    strings: AppStrings,
    onSelectTab: (MainNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMontanuColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(ext.glassFillStart, ext.glassFillEnd)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(ext.glassBorderTop, ext.glassBorderBottom)),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavPillItem(
                selected = currentTab == MainNavTab.SCORES,
                label = strings.navScores,
                selectedIcon = Icons.Filled.SportsSoccer,
                unselectedIcon = Icons.Outlined.SportsSoccer,
                tag = "nav_scores",
                onClick = { onSelectTab(MainNavTab.SCORES) },
                modifier = Modifier.weight(1f)
            )
            NavPillItem(
                selected = currentTab == MainNavTab.SOCIAL,
                label = strings.navSocial,
                selectedIcon = Icons.Filled.Forum,
                unselectedIcon = Icons.Outlined.Forum,
                tag = "nav_social",
                onClick = { onSelectTab(MainNavTab.SOCIAL) },
                modifier = Modifier.weight(1f)
            )
            NavPillItem(
                selected = currentTab == MainNavTab.ARENA,
                label = strings.navArena,
                selectedIcon = Icons.Filled.EmojiEvents,
                unselectedIcon = Icons.Outlined.EmojiEvents,
                tag = "nav_arena",
                onClick = { onSelectTab(MainNavTab.ARENA) },
                modifier = Modifier.weight(1f)
            )
            NavPillItem(
                selected = currentTab == MainNavTab.HUB,
                label = strings.navHub,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                tag = "nav_hub",
                onClick = { onSelectTab(MainNavTab.HUB) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavPillItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillBg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f) else Color.Transparent,
        animationSpec = tween(220),
        label = "navPill"
    )
    val tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(22.dp))
            .background(pillBg)
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TeamBadgeImage(
    logoUrl: String,
    fallbackShort: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    if (logoUrl.isNotBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = "$fallbackShort logo",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackShort.take(3).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LivePulseBadge(label: String) {
    val ext = LocalMontanuColors.current
    val transition = rememberInfiniteTransition(label = "livePulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(ext.liveGreen.copy(alpha = alpha))
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MatchSummaryCard(
    match: MatchSummary,
    liveLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = SimpleDateFormat("MMM d • HH:mm", Locale.getDefault())
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("match_card_${match.id}"),
        highlightBorder = match.status == MatchStatus.LIVE,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.leagueName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                if (match.status == MatchStatus.LIVE) {
                    LivePulseBadge(
                        label = if (match.clockDisplay.isNotBlank()) "$liveLabel • ${match.clockDisplay}" else liveLabel
                    )
                } else {
                    Text(
                        text = match.clockDisplay.ifBlank { dateFmt.format(Date(match.dateEpochMillis)) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (match.sport == SportType.FOOTBALL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = match.home.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        TeamBadgeImage(match.home.logoUrl, match.home.shortName, size = 34.dp)
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val scoreText = if (match.status == MatchStatus.UPCOMING) {
                            "VS"
                        } else {
                            "${match.home.score.ifBlank { "0" }} - ${match.away.score.ifBlank { "0" }}"
                        }
                        Text(
                            text = scoreText,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                            color = if (match.status == MatchStatus.LIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TeamBadgeImage(match.away.logoUrl, match.away.shortName, size = 34.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = match.away.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Cricket two-row scorecard layout
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CricketTeamScoreRow(match.home)
                    CricketTeamScoreRow(match.away)
                }
            }

            if (match.statusNote.isNotBlank() || match.venue.isNotBlank()) {
                Text(
                    text = listOf(match.statusNote, match.venue).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CricketTeamScoreRow(team: com.example.data.model.TeamStub) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            TeamBadgeImage(team.logoUrl, team.shortName, size = 30.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = team.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = team.score.ifBlank { "-" },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatComparisonBar(
    label: String,
    homeValue: String,
    awayValue: String,
    homeRatio: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(homeValue, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(awayValue, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(homeRatio.coerceIn(0.05f, 0.95f))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((1f - homeRatio).coerceIn(0.05f, 0.95f))
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
fun FootballPitchCanvas(
    homeLineup: List<PlayerLineupEntry>,
    awayLineup: List<PlayerLineupEntry>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val homeColor = MaterialTheme.colorScheme.primary
    val awayColor = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0E2A1E))
            .padding(12.dp)
    ) {
        val w = size.width
        val h = size.height
        // Pitch border & halfway line
        drawRect(color = lineColor, style = Stroke(width = 2.dp.toPx()))
        drawLine(color = lineColor, start = Offset(w / 2f, 0f), end = Offset(w / 2f, h), strokeWidth = 2.dp.toPx())
        drawCircle(color = lineColor, radius = h * 0.18f, center = Offset(w / 2f, h / 2f), style = Stroke(2.dp.toPx()))
        drawRect(color = lineColor, topLeft = Offset(0f, h * 0.24f), size = Size(w * 0.14f, h * 0.52f), style = Stroke(2.dp.toPx()))
        drawRect(color = lineColor, topLeft = Offset(w * 0.86f, h * 0.24f), size = Size(w * 0.14f, h * 0.52f), style = Stroke(2.dp.toPx()))

        // Draw up to 11 starters per side
        val hStarters = homeLineup.filter { it.starter }.take(11)
        val aStarters = awayLineup.filter { it.starter }.take(11)
        val homeCoords = listOf(
            0.07f to 0.5f,
            0.20f to 0.2f, 0.20f to 0.4f, 0.20f to 0.6f, 0.20f to 0.8f,
            0.34f to 0.25f, 0.34f to 0.5f, 0.34f to 0.75f,
            0.44f to 0.22f, 0.45f to 0.5f, 0.44f to 0.78f
        )
        hStarters.forEachIndexed { idx, _ ->
            val (rx, ry) = homeCoords.getOrElse(idx) { 0.3f to 0.5f }
            drawCircle(color = homeColor, radius = 8.dp.toPx(), center = Offset(w * rx, h * ry))
        }
        aStarters.forEachIndexed { idx, _ ->
            val (rx, ry) = homeCoords.getOrElse(idx) { 0.3f to 0.5f }
            drawCircle(color = awayColor, radius = 8.dp.toPx(), center = Offset(w * (1f - rx), h * ry))
        }
    }
}

@Composable
fun PlayerHeatmapCanvas(
    zones: List<Float>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0E241C))
            .padding(8.dp)
    ) {
        val w = size.width
        val h = size.height
        drawRect(color = Color.White.copy(alpha = 0.25f), style = Stroke(1.5.dp.toPx()))
        drawLine(Color.White.copy(alpha = 0.25f), Offset(w / 2f, 0f), Offset(w / 2f, h), 1.5.dp.toPx())
        val pts = if (zones.isEmpty()) listOf(0.5f, 0.8f, 0.6f, 0.9f) else zones
        pts.forEachIndexed { index, intensity ->
            val cx = w * ((index + 1f) / (pts.size + 1f))
            val cy = h * (0.28f + ((index % 2) * 0.42f))
            val r = (22.dp.toPx() * intensity.coerceIn(0.3f, 1.1f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.75f), accent.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
fun CricketOverChart(
    points: List<CricketOverPoint>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    if (points.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp)
    ) {
        val maxCum = (points.maxOfOrNull { it.cumulativeRuns } ?: 1).coerceAtLeast(1).toFloat()
        val maxOver = (points.maxOfOrNull { it.runsInOver } ?: 1).coerceAtLeast(1).toFloat()
        val barWidth = (size.width / points.size.coerceAtLeast(1)) * 0.6f
        val path = Path()

        points.forEachIndexed { idx, pt ->
            val x = (idx + 0.5f) * (size.width / points.size)
            val barH = (pt.runsInOver / maxOver) * size.height * 0.55f
            drawRect(
                color = secondary.copy(alpha = 0.45f),
                topLeft = Offset(x - barWidth / 2f, size.height - barH),
                size = Size(barWidth, barH)
            )
            val yLine = size.height - (pt.cumulativeRuns / maxCum) * size.height * 0.9f
            if (idx == 0) path.moveTo(x, yLine) else path.lineTo(x, yLine)
        }
        drawPath(path = path, color = primary, style = Stroke(width = 3.dp.toPx()))
    }
}

fun shareTextCard(context: Context, title: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n\n$body\n\nShared via MONTANU SCORE — Your World of Sports")
    }
    context.startActivity(Intent.createChooser(intent, "Share Match Card"))
}

fun addMatchToCalendar(context: Context, match: MatchSummary) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "${match.home.name} vs ${match.away.name} (${match.leagueName})")
        putExtra(CalendarContract.Events.EVENT_LOCATION, match.venue)
        putExtra(CalendarContract.Events.DESCRIPTION, "MONTANU SCORE Match Reminder — ${match.sport.name}")
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, match.dateEpochMillis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, match.dateEpochMillis + 2 * 60 * 60 * 1000L)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
