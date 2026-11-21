package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.MatchStatus
import com.example.data.model.UiState
import com.example.data.repository.AiAnalysisRepository
import com.example.data.repository.FirestoreSocialRepository
import com.example.data.repository.PreferencesRepository
import com.example.i18n.SUPPORTED_LANGUAGES
import com.example.i18n.stringsForLanguage
import com.example.ui.components.FloatingGlassNavBar
import com.example.ui.components.MainNavTab
import com.example.ui.screens.AuthAndSplashScreen
import com.example.ui.screens.ComparisonToolScreen
import com.example.ui.screens.ExploreAndFeaturesScreen
import com.example.ui.screens.MatchDetailScreen
import com.example.ui.screens.MatchesDashboardScreen
import com.example.ui.screens.ProfileSettingsAndAdminScreen
import com.example.ui.screens.SocialFeedScreen
import com.example.ui.screens.TeamProfileScreen
import com.example.ui.screens.PlayerProfileScreen
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.MontanuScoreTheme
import com.example.ui.viewmodel.SocialHubViewModel
import com.example.ui.viewmodel.SportsViewModel
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private enum class OverlayRoute {
    NONE, MATCH_DETAIL, TEAM_PROFILE, PLAYER_PROFILE, COMPARISON_TOOL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mandatory App Check initialization before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            MontanuAppRoot()
        }
    }
}

@Composable
fun MontanuAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        MontanuScoreTheme(darkTheme = true) {
            AuthAndSplashScreen(
                strings = stringsForLanguage("en"),
                onAuthSuccess = {}
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedMontanuApp(user = user)
        }
    }
}

@Composable
private fun AuthenticatedMontanuApp(user: FirebaseUser) {
    val uid = user.uid
    val sportsVm: SportsViewModel = viewModel(key = "sports_$uid")
    val socialVm: SocialHubViewModel = viewModel(
        key = "social_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                SocialHubViewModel(
                    socialRepo = FirestoreSocialRepository(db),
                    aiRepo = AiAnalysisRepository(),
                    prefsRepo = PreferencesRepository(app),
                    currentUserId = uid,
                    currentUserEmail = user.email.orEmpty(),
                    isEmailVerified = user.isEmailVerified,
                    initialDisplayName = user.displayName ?: "Montanu Fan",
                    initialPhotoUrl = user.photoUrl?.toString()
                )
            }
        }
    )

    val localPrefs by socialVm.localPrefs.collectAsStateWithLifecycle()
    val userAccountState by socialVm.userAccount.collectAsStateWithLifecycle()
    val userAccount = (userAccountState as? UiState.Success)?.data
    val langObj = SUPPORTED_LANGUAGES.firstOrNull { it.code == localPrefs.languageCode } ?: SUPPORTED_LANGUAGES.first()
    val strings = remember(localPrefs.languageCode) { stringsForLanguage(localPrefs.languageCode) }
    val layoutDir = if (langObj.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val selectedSport by sportsVm.selectedSport.collectAsStateWithLifecycle()
    val selectedLeague by sportsVm.selectedLeague.collectAsStateWithLifecycle()
    val statusFilter by sportsVm.statusFilter.collectAsStateWithLifecycle()
    val matchesState by sportsVm.matchesState.collectAsStateWithLifecycle()
    val standingsState by sportsVm.standingsState.collectAsStateWithLifecycle()
    val iccRankings by sportsVm.iccRankings.collectAsStateWithLifecycle()
    val leaders by sportsVm.leaders.collectAsStateWithLifecycle()
    val newsState by sportsVm.newsState.collectAsStateWithLifecycle()
    val matchDetailState by sportsVm.selectedMatchDetail.collectAsStateWithLifecycle()
    val teamProfileState by sportsVm.teamProfileState.collectAsStateWithLifecycle()
    val playerProfileState by sportsVm.playerProfileState.collectAsStateWithLifecycle()
    val compareTeamA by sportsVm.compareTeamA.collectAsStateWithLifecycle()
    val compareTeamB by sportsVm.compareTeamB.collectAsStateWithLifecycle()
    val comparePlayerA by sportsVm.comparePlayerA.collectAsStateWithLifecycle()
    val comparePlayerB by sportsVm.comparePlayerB.collectAsStateWithLifecycle()

    val feedPostsState by socialVm.feedPosts.collectAsStateWithLifecycle()
    val profilesState by socialVm.publicProfiles.collectAsStateWithLifecycle()
    val publicGroupsState by socialVm.publicGroups.collectAsStateWithLifecycle()
    val myGroupsState by socialVm.myGroups.collectAsStateWithLifecycle()
    val chatsState by socialVm.directChats.collectAsStateWithLifecycle()
    val bookmarksState by socialVm.bookmarks.collectAsStateWithLifecycle()
    val followingState by socialVm.following.collectAsStateWithLifecycle()
    val friendRequestsState by socialVm.friendRequests.collectAsStateWithLifecycle()
    val notificationsState by socialVm.notifications.collectAsStateWithLifecycle()
    val leaderboardState by socialVm.leaderboard.collectAsStateWithLifecycle()
    val appConfigState by socialVm.appConfig.collectAsStateWithLifecycle()
    val reportsState by socialVm.reports.collectAsStateWithLifecycle()
    val postCommentsState by socialVm.postComments.collectAsStateWithLifecycle()
    val groupMessagesState by socialVm.groupMessages.collectAsStateWithLifecycle()
    val chatMessagesState by socialVm.chatMessages.collectAsStateWithLifecycle()
    val aiMatchInsight by socialVm.aiMatchInsight.collectAsStateWithLifecycle()
    val aiPlayerInsight by socialVm.aiPlayerInsight.collectAsStateWithLifecycle()
    val triviaState by socialVm.triviaQuestions.collectAsStateWithLifecycle()
    val toastMsg by socialVm.toastMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainNavTab.SCORES) }
    var overlay by remember { mutableStateOf(OverlayRoute.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = overlay != OverlayRoute.NONE) {
        overlay = OverlayRoute.NONE
    }
    BackHandler(enabled = overlay == OverlayRoute.NONE && currentTab != MainNavTab.SCORES) {
        currentTab = MainNavTab.SCORES
    }

    LaunchedEffect(toastMsg) {
        val msg = toastMsg
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            socialVm.clearToast()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        MontanuScoreTheme(
            darkTheme = localPrefs.darkMode,
            highContrast = localPrefs.highContrast
        ) {
            LiquidGlassBackground(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = innerPadding.calculateTopPadding())
                    ) {
                        when (overlay) {
                            OverlayRoute.MATCH_DETAIL -> {
                                val detail = matchDetailState ?: UiState.Loading
                                MatchDetailScreen(
                                    detailState = detail,
                                    aiInsightState = aiMatchInsight,
                                    strings = strings,
                                    onBack = { overlay = OverlayRoute.NONE },
                                    onRunAiAnalysis = { socialVm.analyzeMatch(it) },
                                    onOpenTeam = { teamName ->
                                        sportsVm.openTeamProfile(teamName)
                                        overlay = OverlayRoute.TEAM_PROFILE
                                    },
                                    onOpenPlayer = { playerName ->
                                        sportsVm.openPlayerProfile(playerName)
                                        overlay = OverlayRoute.PLAYER_PROFILE
                                    },
                                    onSaveOffline = { d ->
                                        socialVm.saveBookmark(
                                            type = "MATCH",
                                            itemId = d.summary.id,
                                            sport = d.summary.sport.name,
                                            title = "${d.summary.home.name} vs ${d.summary.away.name}",
                                            subtitle = "${d.summary.home.score} - ${d.summary.away.score} (${d.summary.leagueName})",
                                            payloadJson = d.stats.joinToString("; ") { "${it.label}: ${it.homeValue}-${it.awayValue}" }
                                        )
                                    }
                                )
                            }

                            OverlayRoute.TEAM_PROFILE -> {
                                val tpState = teamProfileState ?: UiState.Loading
                                val teamName = (tpState as? UiState.Success)?.data?.name.orEmpty()
                                TeamProfileScreen(
                                    teamState = tpState,
                                    strings = strings,
                                    isFavourite = teamName in (userAccount?.favTeamIds ?: emptyList()),
                                    onToggleFavourite = { socialVm.toggleFavTeam(it) },
                                    onOpenPlayer = { playerName ->
                                        sportsVm.openPlayerProfile(playerName)
                                        overlay = OverlayRoute.PLAYER_PROFILE
                                    },
                                    onBack = { overlay = OverlayRoute.NONE }
                                )
                            }

                            OverlayRoute.PLAYER_PROFILE -> {
                                val ppState = playerProfileState ?: UiState.Loading
                                val playerName = (ppState as? UiState.Success)?.data?.fullName.orEmpty()
                                PlayerProfileScreen(
                                    playerState = ppState,
                                    aiInsightState = aiPlayerInsight,
                                    strings = strings,
                                    isFavourite = playerName in (userAccount?.favPlayerIds ?: emptyList()),
                                    onToggleFavourite = { socialVm.toggleFavPlayer(it) },
                                    onRunAiAnalysis = { socialVm.analyzePlayer(it) },
                                    onBack = { overlay = OverlayRoute.NONE }
                                )
                            }

                            OverlayRoute.COMPARISON_TOOL -> {
                                ComparisonToolScreen(
                                    teamA = compareTeamA,
                                    teamB = compareTeamB,
                                    playerA = comparePlayerA,
                                    playerB = comparePlayerB,
                                    strings = strings,
                                    onCompareTeams = { a, b -> sportsVm.loadComparisonTeams(a, b) },
                                    onComparePlayers = { a, b -> sportsVm.loadComparisonPlayers(a, b) },
                                    onBack = { overlay = OverlayRoute.NONE }
                                )
                            }

                            OverlayRoute.NONE -> {
                                when (currentTab) {
                                    MainNavTab.SCORES -> MatchesDashboardScreen(
                                        sportsVm = sportsVm,
                                        strings = strings,
                                        selectedSport = selectedSport,
                                        selectedLeague = selectedLeague,
                                        statusFilter = statusFilter,
                                        matchesState = matchesState,
                                        standingsState = standingsState,
                                        iccRankings = iccRankings,
                                        leaders = leaders,
                                        newsState = newsState,
                                        onOpenMatch = { match ->
                                            sportsVm.openMatchDetail(match)
                                            overlay = OverlayRoute.MATCH_DETAIL
                                        },
                                        onOpenTeam = { teamName ->
                                            sportsVm.openTeamProfile(teamName)
                                            overlay = OverlayRoute.TEAM_PROFILE
                                        },
                                        onOpenPlayer = { playerName ->
                                            sportsVm.openPlayerProfile(playerName)
                                            overlay = OverlayRoute.PLAYER_PROFILE
                                        },
                                        onBookmarkNews = { article ->
                                            socialVm.saveBookmark(
                                                type = "NEWS",
                                                itemId = article.id,
                                                sport = article.sport.name,
                                                title = article.headline,
                                                subtitle = article.category
                                            )
                                        },
                                        modifier = Modifier.statusBarsPadding()
                                    )

                                    MainNavTab.SOCIAL -> SocialFeedScreen(
                                        vm = socialVm,
                                        strings = strings,
                                        feedState = feedPostsState,
                                        profilesState = profilesState,
                                        publicGroupsState = publicGroupsState,
                                        myGroupsState = myGroupsState,
                                        chatsState = chatsState,
                                        followingState = followingState,
                                        friendRequestsState = friendRequestsState,
                                        postCommentsState = postCommentsState,
                                        groupMessagesState = groupMessagesState,
                                        chatMessagesState = chatMessagesState,
                                        blockedUids = userAccount?.blockedUserIds ?: emptyList(),
                                        modifier = Modifier.statusBarsPadding()
                                    )

                                    MainNavTab.ARENA -> {
                                        val matches = (matchesState as? UiState.Success)?.data.orEmpty()
                                        val featured = matches.firstOrNull { it.status == MatchStatus.LIVE } ?: matches.firstOrNull()
                                        ExploreAndFeaturesScreen(
                                            vm = socialVm,
                                            strings = strings,
                                            userAccount = userAccount,
                                            triviaState = triviaState,
                                            leaderboardState = leaderboardState,
                                            bookmarksState = bookmarksState,
                                            notificationsState = notificationsState,
                                            featuredLiveMatch = featured,
                                            onOpenComparisonTool = { overlay = OverlayRoute.COMPARISON_TOOL },
                                            onOpenMatch = { match ->
                                                sportsVm.openMatchDetail(match)
                                                overlay = OverlayRoute.MATCH_DETAIL
                                            },
                                            modifier = Modifier.statusBarsPadding()
                                        )
                                    }

                                    MainNavTab.HUB -> ProfileSettingsAndAdminScreen(
                                        vm = socialVm,
                                        strings = strings,
                                        localPrefs = localPrefs,
                                        userAccount = userAccount,
                                        profilesState = profilesState,
                                        postsState = feedPostsState,
                                        groupsState = publicGroupsState,
                                        reportsState = reportsState,
                                        appConfigState = appConfigState,
                                        apiDiagnostics = sportsVm.apiDiagnostics,
                                        onSignOutComplete = {},
                                        modifier = Modifier.statusBarsPadding()
                                    )
                                }

                                FloatingGlassNavBar(
                                    currentTab = currentTab,
                                    strings = strings,
                                    onSelectTab = { currentTab = it },
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
