package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppConfig
import com.example.data.model.BookmarkItem
import com.example.data.model.ChatMessage
import com.example.data.model.DirectChat
import com.example.data.model.FollowLink
import com.example.data.model.FriendRequest
import com.example.data.model.GroupMessage
import com.example.data.model.MatchDetail
import com.example.data.model.ModerationReport
import com.example.data.model.PlayerProfile
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.SocialPost
import com.example.data.model.SportType
import com.example.data.model.SportsGroup
import com.example.data.model.TriviaScore
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.UserNotification
import com.example.data.repository.AiAnalysisRepository
import com.example.data.repository.FirestoreSocialRepository
import com.example.data.repository.LocalAppPrefs
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.TriviaQuestion
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocialHubViewModel(
    private val socialRepo: FirestoreSocialRepository,
    private val aiRepo: AiAnalysisRepository,
    private val prefsRepo: PreferencesRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val isEmailVerified: Boolean,
    val initialDisplayName: String,
    val initialPhotoUrl: String?
) : ViewModel() {

    val isAdmin: Boolean =
        isEmailVerified && currentUserEmail.equals("monayim651@gmail.com", ignoreCase = true)

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val localPrefs: StateFlow<LocalAppPrefs> = prefsRepo.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalAppPrefs())

    val userAccount: StateFlow<UiState<UserAccount?>> = safeUiState(
        socialRepo.observeUserAccount(currentUserId),
        "Failed to load account"
    )

    val feedPosts: StateFlow<UiState<List<SocialPost>>> = safeUiState(
        socialRepo.observeFeedPosts(),
        "Failed to load social feed"
    )

    val publicProfiles: StateFlow<UiState<List<PublicProfile>>> = safeUiState(
        socialRepo.observePublicProfiles(),
        "Failed to load fan profiles"
    )

    val publicGroups: StateFlow<UiState<List<SportsGroup>>> = safeUiState(
        socialRepo.observePublicGroups(),
        "Failed to load communities"
    )

    val myGroups: StateFlow<UiState<List<SportsGroup>>> = safeUiState(
        socialRepo.observeMyGroups(currentUserId),
        "Failed to load my groups"
    )

    val directChats: StateFlow<UiState<List<DirectChat>>> = safeUiState(
        socialRepo.observeDirectChats(currentUserId),
        "Failed to load messages"
    )

    val bookmarks: StateFlow<UiState<List<BookmarkItem>>> = safeUiState(
        socialRepo.observeBookmarks(currentUserId),
        "Failed to load bookmarks"
    )

    val following: StateFlow<UiState<List<FollowLink>>> = safeUiState(
        socialRepo.observeFollowing(currentUserId),
        "Failed to load following list"
    )

    val friendRequests: StateFlow<UiState<List<FriendRequest>>> = safeUiState(
        socialRepo.observeFriendRequests(currentUserId),
        "Failed to load friend requests"
    )

    val notifications: StateFlow<UiState<List<UserNotification>>> = safeUiState(
        socialRepo.observeNotifications(currentUserId),
        "Failed to load notifications"
    )

    val leaderboard: StateFlow<UiState<List<TriviaScore>>> = safeUiState(
        socialRepo.observeLeaderboard(),
        "Failed to load leaderboard"
    )

    val appConfig: StateFlow<UiState<AppConfig?>> = safeUiState(
        socialRepo.observeAppConfig(),
        "Failed to check update centre"
    )

    val reports: StateFlow<UiState<List<ModerationReport>>> = safeUiState(
        socialRepo.observeReports(currentUserId, isAdmin),
        "Failed to load reports"
    )

    private val _selectedPostId = MutableStateFlow<String?>(null)
    val postComments: StateFlow<UiState<List<PostComment>>> = _selectedPostId.flatMapLatest { pid ->
        if (pid.isNullOrBlank()) flowOf(UiState.Success(emptyList()))
        else socialRepo.observePostComments(pid).map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
            .catch { emit(UiState.Error(unwrapFirestoreMsg(it, "Failed to load comments"))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    private val _selectedGroup = MutableStateFlow<SportsGroup?>(null)
    val groupMessages: StateFlow<UiState<List<GroupMessage>>> = _selectedGroup.flatMapLatest { grp ->
        if (grp == null) flowOf(UiState.Success(emptyList()))
        else socialRepo.observeGroupMessages(grp, currentUserId)
            .map<List<GroupMessage>, UiState<List<GroupMessage>>> { UiState.Success(it) }
            .catch { emit(UiState.Error(unwrapFirestoreMsg(it, "Failed to load group chat"))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val chatMessages: StateFlow<UiState<List<ChatMessage>>> = _selectedChatId.flatMapLatest { cid ->
        if (cid.isNullOrBlank()) flowOf(UiState.Success(emptyList()))
        else socialRepo.observeChatMessages(cid, currentUserId)
            .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
            .catch { emit(UiState.Error(unwrapFirestoreMsg(it, "Failed to load chat"))) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    // AI States
    private val _aiMatchInsight = MutableStateFlow<UiState<String>?>(null)
    val aiMatchInsight: StateFlow<UiState<String>?> = _aiMatchInsight.asStateFlow()

    private val _aiPlayerInsight = MutableStateFlow<UiState<String>?>(null)
    val aiPlayerInsight: StateFlow<UiState<String>?> = _aiPlayerInsight.asStateFlow()

    private val _triviaQuestions = MutableStateFlow<UiState<List<TriviaQuestion>>?>(null)
    val triviaQuestions: StateFlow<UiState<List<TriviaQuestion>>?> = _triviaQuestions.asStateFlow()

    init {
        viewModelScope.launch {
            socialRepo.ensureUserInitialized(
                uid = currentUserId,
                email = currentUserEmail,
                displayName = initialDisplayName,
                avatarUrl = initialPhotoUrl
            )
        }
    }

    private fun <T> safeUiState(source: Flow<T>, fallbackMsg: String): StateFlow<UiState<T>> {
        return source
            .map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                emit(UiState.Error(root.message ?: fallbackMsg))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)
    }

    private fun unwrapFirestoreMsg(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return (firestoreError ?: error).message ?: fallback
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun selectPostForComments(postId: String?) {
        _selectedPostId.value = postId
    }

    fun selectGroup(group: SportsGroup?) {
        _selectedGroup.value = group
    }

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
    }

    // --- SOCIAL ACTIONS ---

    fun createPost(
        sportTag: String,
        mediaType: String,
        content: String,
        mediaUrl: String? = null,
        matchRefId: String? = null,
        matchLabel: String? = null,
        pollOptionA: String? = null,
        pollOptionB: String? = null
    ) {
        viewModelScope.launch {
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            val myProfile = (publicProfiles.value as? UiState.Success)?.data?.firstOrNull { it.userId == currentUserId }
            val res = socialRepo.createPost(
                authorName = myName,
                authorAvatar = myProfile?.avatarUrl ?: initialPhotoUrl,
                authorVerified = myProfile?.verified ?: false,
                sportTag = sportTag,
                mediaType = mediaType,
                content = content,
                mediaUrl = mediaUrl,
                matchRefId = matchRefId,
                matchLabel = matchLabel,
                pollOptionA = pollOptionA,
                pollOptionB = pollOptionB
            )
            _toastMessage.value = if (res.isSuccess) "Posted to MONTANU Social!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val res = socialRepo.toggleLikePost(postId)
            if (res.isFailure) _toastMessage.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun votePoll(postId: String, optionA: Boolean) {
        viewModelScope.launch {
            val res = socialRepo.votePoll(postId, optionA)
            _toastMessage.value = if (res.isSuccess) "Vote recorded!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun sharePost(postId: String) {
        viewModelScope.launch {
            socialRepo.incrementShare(postId)
        }
    }

    fun addComment(postId: String, content: String, parentCommentId: String? = null) {
        viewModelScope.launch {
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            val res = socialRepo.addComment(postId, myName, content, parentCommentId)
            if (res.isFailure) _toastMessage.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun createGroup(name: String, desc: String, sportTag: String, kind: String, privacy: String) {
        viewModelScope.launch {
            val res = socialRepo.createGroup(name, desc, sportTag, kind, privacy)
            _toastMessage.value = if (res.isSuccess) "Community created!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun joinGroup(group: SportsGroup) {
        viewModelScope.launch {
            val res = socialRepo.joinPublicGroup(group)
            _toastMessage.value = if (res.isSuccess) "Joined ${group.name}" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun sendGroupMessage(group: SportsGroup, text: String) {
        viewModelScope.launch {
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            val res = socialRepo.sendGroupMessage(group, myName, text)
            if (res.isFailure) _toastMessage.value = res.exceptionOrNull()?.localizedMessage
        }
    }

    fun sendDirectMessage(otherUid: String, otherName: String, text: String) {
        viewModelScope.launch {
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            val res = socialRepo.startOrSendDirectChat(otherUid, otherName, myName, text)
            res.onSuccess { chatId -> _selectedChatId.value = chatId }
            res.onFailure { _toastMessage.value = it.localizedMessage }
        }
    }

    fun toggleFollow(target: PublicProfile, isFollowing: Boolean) {
        viewModelScope.launch {
            val res = socialRepo.toggleFollow(target, isFollowing)
            _toastMessage.value = if (res.isSuccess) {
                if (isFollowing) "Unfollowed ${target.displayName}" else "Following ${target.displayName}"
            } else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun sendFriendRequest(targetUid: String) {
        viewModelScope.launch {
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            val res = socialRepo.sendFriendRequest(targetUid, myName)
            _toastMessage.value = if (res.isSuccess) "Friend request sent!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun respondFriendRequest(requesterUid: String, accept: Boolean) {
        viewModelScope.launch {
            val res = socialRepo.respondFriendRequest(requesterUid, accept)
            _toastMessage.value = if (res.isSuccess) {
                if (accept) "Friend request accepted!" else "Friend request declined."
            } else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveBookmark(type: String, itemId: String, sport: String, title: String, subtitle: String, payloadJson: String? = null) {
        viewModelScope.launch {
            val res = socialRepo.saveBookmark(type, itemId, sport, title, subtitle, payloadJson)
            _toastMessage.value = if (res.isSuccess) "Saved to Bookmarks & Offline Vault" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun removeBookmark(docId: String) {
        viewModelScope.launch {
            socialRepo.removeBookmark(docId)
        }
    }

    fun reportContent(targetType: String, targetId: String, reason: String) {
        viewModelScope.launch {
            val res = socialRepo.reportTarget(targetType, targetId, reason)
            _toastMessage.value = if (res.isSuccess) "Report submitted to moderators." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun blockUser(blockedUid: String) {
        viewModelScope.launch {
            val res = socialRepo.blockUser(blockedUid)
            _toastMessage.value = if (res.isSuccess) "User blocked." else res.exceptionOrNull()?.localizedMessage
        }
    }

    // --- PREFERENCES & SETTINGS ---

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDarkMode(enabled)
            socialRepo.updateUserPreferences(darkMode = enabled)
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            prefsRepo.setLanguage(code)
            socialRepo.updateUserPreferences(languageCode = code)
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setHighContrast(enabled)
            socialRepo.updateUserPreferences(highContrast = enabled)
        }
    }

    fun toggleFavTeam(teamName: String) {
        viewModelScope.launch {
            val current = (userAccount.value as? UiState.Success)?.data?.favTeamIds ?: emptyList()
            val res = socialRepo.updateUserPreferences(favTeamToggle = teamName, currentFavTeams = current)
            _toastMessage.value = if (res.isSuccess) {
                if (teamName in current) "Removed $teamName from favourites" else "Added $teamName to favourites"
            } else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun toggleFavPlayer(playerName: String) {
        viewModelScope.launch {
            val current = (userAccount.value as? UiState.Success)?.data?.favPlayerIds ?: emptyList()
            val res = socialRepo.updateUserPreferences(favPlayerToggle = playerName, currentFavPlayers = current)
            _toastMessage.value = if (res.isSuccess) {
                if (playerName in current) "Removed $playerName from favourites" else "Added $playerName to favourites"
            } else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun updateProfile(displayName: String, bio: String, badge: String) {
        viewModelScope.launch {
            val res = socialRepo.updatePublicProfile(displayName, bio, badge)
            _toastMessage.value = if (res.isSuccess) "Profile updated!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun submitFeedback(subject: String, message: String) {
        viewModelScope.launch {
            val res = socialRepo.submitFeedback(subject, message)
            _toastMessage.value = if (res.isSuccess) "Thank you! Feedback sent." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun deleteAccountData(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val res = socialRepo.deleteAccountData()
            if (res.isSuccess) {
                onDeleted()
            } else {
                _toastMessage.value = res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    // --- AI LOGIC (GEMINI 3.8 FLASH) ---

    fun analyzeMatch(detail: MatchDetail) {
        viewModelScope.launch {
            _aiMatchInsight.value = UiState.Loading
            try {
                aiRepo.streamMatchAnalysis(detail).collect { partial ->
                    _aiMatchInsight.value = UiState.Success(partial)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiMatchInsight.value = UiState.Error(e.localizedMessage ?: "AI Match Analysis unavailable.")
            }
        }
    }

    fun analyzePlayer(player: PlayerProfile) {
        viewModelScope.launch {
            _aiPlayerInsight.value = UiState.Loading
            try {
                aiRepo.streamPlayerAnalysis(player).collect { partial ->
                    _aiPlayerInsight.value = UiState.Success(partial)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiPlayerInsight.value = UiState.Error(e.localizedMessage ?: "AI Player Analysis unavailable.")
            }
        }
    }

    fun loadDailyTrivia(sport: SportType) {
        viewModelScope.launch {
            _triviaQuestions.value = UiState.Loading
            val res = aiRepo.generateDailyTrivia(sport)
            _triviaQuestions.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "Failed to generate trivia.") }
            )
        }
    }

    fun submitTriviaScore(points: Long, correct: Long) {
        viewModelScope.launch {
            val myName = (userAccount.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
            val res = socialRepo.recordTriviaResult(myName, points, correct)
            _toastMessage.value = if (res.isSuccess) "+$points pts added to Fan Leaderboard!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    // --- ADMIN CONTROLS ---

    fun adminPublishConfig(
        versionCode: Long,
        versionName: String,
        mandatory: Boolean,
        maintenance: Boolean,
        whatsNew: String,
        title: String,
        body: String
    ) {
        viewModelScope.launch {
            val res = socialRepo.adminPublishUpdateConfig(versionCode, versionName, mandatory, maintenance, whatsNew, title, body)
            _toastMessage.value = if (res.isSuccess) "Update config published!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun adminModeratePost(postId: String, status: String) {
        viewModelScope.launch {
            val res = socialRepo.adminModeratePost(postId, status)
            _toastMessage.value = if (res.isSuccess) "Post status -> $status" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun adminResolveReport(reportId: String, status: String) {
        viewModelScope.launch {
            val res = socialRepo.adminResolveReport(reportId, status)
            _toastMessage.value = if (res.isSuccess) "Report marked $status" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun adminVerifyUser(targetUid: String, verified: Boolean) {
        viewModelScope.launch {
            val res = socialRepo.adminToggleVerifyUser(targetUid, verified)
            _toastMessage.value = if (res.isSuccess) "User verification updated" else res.exceptionOrNull()?.localizedMessage
        }
    }
}
