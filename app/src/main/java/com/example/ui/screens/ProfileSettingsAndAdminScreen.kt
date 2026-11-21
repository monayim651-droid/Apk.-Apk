package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.BuildConfig
import com.example.data.model.AppConfig
import com.example.data.model.ModerationReport
import com.example.data.model.PublicProfile
import com.example.data.model.SocialPost
import com.example.data.model.SportsGroup
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.repository.LocalAppPrefs
import com.example.data.repository.SportsRepository
import com.example.i18n.AppStrings
import com.example.i18n.SUPPORTED_LANGUAGES
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.viewmodel.SocialHubViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileSettingsAndAdminScreen(
    vm: SocialHubViewModel,
    strings: AppStrings,
    localPrefs: LocalAppPrefs,
    userAccount: UserAccount?,
    profilesState: UiState<List<PublicProfile>>,
    postsState: UiState<List<SocialPost>>,
    groupsState: UiState<List<SportsGroup>>,
    reportsState: UiState<List<ModerationReport>>,
    appConfigState: UiState<AppConfig?>,
    apiDiagnostics: SportsRepository.ApiDiagnostics,
    onSignOutComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAdminSection by remember { mutableStateOf(false) }

    val myProfile = (profilesState as? UiState.Success)?.data?.firstOrNull { it.userId == vm.currentUserId }
    val appConfig = (appConfigState as? UiState.Success)?.data
    val currentLang = SUPPORTED_LANGUAGES.firstOrNull { it.code == localPrefs.languageCode } ?: SUPPORTED_LANGUAGES.first()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(strings.navHub, style = MaterialTheme.typography.headlineMedium)
        }

        // User Profile Summary Card
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = myProfile?.displayName ?: userAccount?.displayName ?: vm.initialDisplayName,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "${myProfile?.handle ?: "@fan"} • ${myProfile?.fanBadge ?: "Founding Fan"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(vm.currentUserEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (!myProfile?.bio.isNullOrBlank()) {
                        Text(myProfile.bio.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = "Followers: ${myProfile?.followersCount ?: 0} • Fav Teams: ${userAccount?.favTeamIds?.size ?: 0} • Fav Players: ${userAccount?.favPlayerIds?.size ?: 0}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Theme, Accessibility & 23+ Language Settings
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.settings, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (localPrefs.darkMode) strings.darkMode else strings.lightMode, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = localPrefs.darkMode,
                            onCheckedChange = { vm.setDarkMode(it) },
                            modifier = Modifier.testTag("theme_mode_switch")
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.highContrast, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = localPrefs.highContrast,
                            onCheckedChange = { vm.setHighContrast(it) }
                        )
                    }
                    OutlinedButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("language_picker_button")
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("${strings.language}: ${currentLang.nativeName} (${currentLang.englishName})")
                    }
                }
            }
        }

        // App Update Centre
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.updateCentre, style = MaterialTheme.typography.titleLarge)
                    }
                    val remoteCode = appConfig?.latestVersionCode ?: BuildConfig.VERSION_CODE.toLong()
                    val remoteName = appConfig?.latestVersionName ?: BuildConfig.VERSION_NAME
                    val hasUpdate = remoteCode > BuildConfig.VERSION_CODE.toLong()
                    Text(
                        text = if (hasUpdate) "New version available: v$remoteName (Build $remoteCode)" else "Up to date: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${strings.whatsNew}: ${appConfig?.whatsNewNotes ?: "Liquid Glass UI, Live Football & Cricket APIs, Gemini AI Analysis, 23+ Languages."}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (appConfig?.maintenanceMode == true) {
                        Text("Maintenance Notice: ${appConfig.announcementBody}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = {
                            val url = appConfig?.playStoreUrl ?: "https://play.google.com/store/apps"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.checkUpdates)
                    }
                }
            }
        }

        // Admin Command Center (visible for verified admin)
        if (vm.isAdmin) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), highlightBorder = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.adminDashboard, style = MaterialTheme.typography.titleLarge)
                            }
                            Switch(checked = showAdminSection, onCheckedChange = { showAdminSection = it })
                        }

                        val usersCount = (profilesState as? UiState.Success)?.data?.size ?: 0
                        val postsCount = (postsState as? UiState.Success)?.data?.size ?: 0
                        val groupsCount = (groupsState as? UiState.Success)?.data?.size ?: 0
                        val openReps = (reportsState as? UiState.Success)?.data?.count { it.status == "OPEN" } ?: 0

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${strings.totalUsers}: $usersCount", style = MaterialTheme.typography.labelLarge)
                            Text("${strings.activePosts}: $postsCount", style = MaterialTheme.typography.labelLarge)
                            Text("${strings.communityGroups}: $groupsCount", style = MaterialTheme.typography.labelLarge)
                            Text("${strings.openReports}: $openReps", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "API Health — ESPN Football: ${apiDiagnostics.espnFootballOk} | ESPN Cricket: ${apiDiagnostics.espnCricketOk} | SportsDB: ${apiDiagnostics.sportsDbOk}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showAdminSection) {
                item {
                    AdminPublishUpdateCard(vm, strings)
                }
                val openReportsList = (reportsState as? UiState.Success)?.data.orEmpty().filter { it.status == "OPEN" }
                items(openReportsList, key = { it.id }) { rep ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Report: ${rep.targetType} (${rep.targetId})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text(rep.reason, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    if (rep.targetType == "POST") vm.adminModeratePost(rep.targetId, "REMOVED")
                                    vm.adminResolveReport(rep.id, "RESOLVED")
                                }) { Text("Remove & Resolve") }
                                OutlinedButton(onClick = { vm.adminResolveReport(rep.id, "DISMISSED") }) { Text("Dismiss") }
                            }
                        }
                    }
                }
            }
        }

        // Feedback, Privacy, Delete Account & Sign Out
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { showFeedbackDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Feedback, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.feedback)
                    }
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.deleteAccount, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                withContext(NonCancellable) {
                                    try {
                                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                    } catch (_: Exception) {}
                                    finally {
                                        Firebase.auth.signOut()
                                        onSignOutComplete()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                    ) {
                        Text(strings.signOut, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.language) },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(SUPPORTED_LANGUAGES) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.setLanguage(lang.code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${lang.nativeName} (${lang.englishName})", style = MaterialTheme.typography.bodyLarge)
                            if (lang.code == localPrefs.languageCode) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showEditProfileDialog) {
        var name by remember { mutableStateOf(myProfile?.displayName ?: vm.initialDisplayName) }
        var bio by remember { mutableStateOf(myProfile?.bio.orEmpty()) }
        var badge by remember { mutableStateOf(myProfile?.fanBadge ?: "Founding Fan") }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") }, singleLine = true)
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") })
                    OutlinedTextField(value = badge, onValueChange = { badge = it }, label = { Text("Fan Badge") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.updateProfile(name, bio, badge)
                    showEditProfileDialog = false
                }) { Text(strings.submit) }
            },
            dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showFeedbackDialog) {
        var subject by remember { mutableStateOf("") }
        var msg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text(strings.feedback) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true)
                    OutlinedTextField(value = msg, onValueChange = { msg = it }, label = { Text("Message") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.submitFeedback(subject, msg)
                    showFeedbackDialog = false
                }) { Text(strings.submit) }
            },
            dismissButton = { TextButton(onClick = { showFeedbackDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteAccount) },
            text = { Text("This deletes your MONTANU SCORE profile, trivia score, and private settings from Firestore and signs you out.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        vm.deleteAccountData {
                            scope.launch {
                                withContext(NonCancellable) {
                                    Firebase.auth.signOut()
                                    onSignOutComplete()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(strings.cancel) } }
        )
    }
}

@Composable
private fun AdminPublishUpdateCard(vm: SocialHubViewModel, strings: AppStrings) {
    var versionName by remember { mutableStateOf("1.1.0") }
    var versionCode by remember { mutableStateOf("2") }
    var notes by remember { mutableStateOf("Live Cricket & Football APIs + Liquid Glass v1.1") }
    var mandatory by remember { mutableStateOf(false) }
    var maintenance by remember { mutableStateOf(false) }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.broadcastUpdate, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = versionName, onValueChange = { versionName = it }, label = { Text("Version") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = versionCode, onValueChange = { versionCode = it }, label = { Text("Code") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("What's New / Notice") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Mandatory Update")
                Switch(checked = mandatory, onCheckedChange = { mandatory = it })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Maintenance Mode")
                Switch(checked = maintenance, onCheckedChange = { maintenance = it })
            }
            Button(
                onClick = {
                    vm.adminPublishConfig(
                        versionCode = versionCode.toLongOrNull() ?: 2L,
                        versionName = versionName,
                        mandatory = mandatory,
                        maintenance = maintenance,
                        whatsNew = notes,
                        title = "MONTANU SCORE $versionName",
                        body = notes
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Publish to Update Centre") }
        }
    }
}
