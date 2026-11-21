package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val darkMode: Boolean = true,
    val languageCode: String = "en",
    val favTeamIds: List<String> = emptyList(),
    val favPlayerIds: List<String> = emptyList(),
    val blockedUserIds: List<String> = emptyList(),
    val triviaPoints: Long = 0L,
    val triviaStreak: Long = 0L,
    val achievements: List<String> = emptyList(),
    val highContrast: Boolean = false,
    val matchRemindersEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val bio: String? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val fanBadge: String = "Rookie",
    val verified: Boolean = false,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val onlineStatus: String = "ONLINE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BookmarkItem(
    @DocumentId val id: String = "",
    val bookmarkType: String = "MATCH",
    val itemId: String = "",
    val sport: String = "FOOTBALL",
    val title: String = "",
    val subtitle: String = "",
    val payloadJson: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowLink(
    @DocumentId val id: String = "",
    val targetUid: String = "",
    val targetName: String = "",
    val targetAvatar: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val requesterAvatar: String? = null,
    val status: String = "PENDING",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "SOCIAL",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val authorVerified: Boolean = false,
    val sportTag: String = "FOOTBALL",
    val mediaType: String = "TEXT",
    val content: String = "",
    val mediaUrl: String? = null,
    val matchRefId: String? = null,
    val matchLabel: String? = null,
    val hashtags: List<String> = emptyList(),
    val pollOptionA: String? = null,
    val pollOptionB: String? = null,
    val votesA: Long = 0L,
    val votesB: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val parentCommentId: String? = null,
    val content: String = "",
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SportsGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val sportTag: String = "FOOTBALL",
    val groupKind: String = "COMMUNITY",
    val privacy: String = "PUBLIC",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val memberCount: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val privacy: String = "PUBLIC",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectChat(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
    val lastMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "POST",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TriviaScore(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val points: Long = 0L,
    val correctAnswers: Long = 0L,
    val streak: Long = 0L,
    val badge: String = "Rookie",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppConfig(
    @DocumentId val id: String = "",
    val latestVersionCode: Long = 1L,
    val latestVersionName: String = "1.0",
    val mandatoryUpdate: Boolean = false,
    val maintenanceMode: Boolean = false,
    val whatsNewNotes: String = "Liquid Glass UI, Live Football & Cricket APIs, AI Match Analysis & 23+ Languages.",
    val announcementTitle: String = "Welcome to MONTANU SCORE",
    val announcementBody: String = "Real-time Football & Cricket data, social fan clubs, and Gemini AI insights.",
    val playStoreUrl: String = "https://play.google.com/store/apps/details?id=com.aistudio.montanuscore.vxqrmt",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserFeedback(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
