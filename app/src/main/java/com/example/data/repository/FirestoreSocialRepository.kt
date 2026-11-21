package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.BookmarkItem
import com.example.data.model.ChatMessage
import com.example.data.model.DirectChat
import com.example.data.model.FollowLink
import com.example.data.model.FriendRequest
import com.example.data.model.GroupMessage
import com.example.data.model.ModerationReport
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.SocialPost
import com.example.data.model.SportsGroup
import com.example.data.model.TriviaScore
import com.example.data.model.UserAccount
import com.example.data.model.UserFeedback
import com.example.data.model.UserNotification
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreSocialRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun unwrapAndLogError(error: Throwable, op: OperationType, path: String, expectedUid: String? = null) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun ensureUserInitialized(
        uid: String,
        email: String,
        displayName: String,
        avatarUrl: String?
    ): Result<Unit> {
        val userPath = "users/$uid"
        val profilePath = "public_profiles/$uid"
        return try {
            val activeUid = requireUserId()
            check(activeUid == uid) { "Signed-in user mismatch." }

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val safeName = displayName.ifBlank { "Montanu Fan" }.take(60)
                val userPayload = mapOf(
                    "userId" to uid,
                    "email" to email.take(200),
                    "displayName" to safeName,
                    "darkMode" to true,
                    "languageCode" to "en",
                    "favTeamIds" to emptyList<String>(),
                    "favPlayerIds" to emptyList<String>(),
                    "blockedUserIds" to emptyList<String>(),
                    "triviaPoints" to 0L,
                    "triviaStreak" to 0L,
                    "achievements" to listOf("FIRST_SIGN_IN"),
                    "highContrast" to false,
                    "matchRemindersEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(userPayload).await()
            }

            val profileRef = db.collection("public_profiles").document(uid)
            val profileSnap = profileRef.get().await()
            if (!profileSnap.exists()) {
                val safeName = displayName.ifBlank { "Montanu Fan" }.take(60)
                val handleBase = safeName.lowercase().replace(Regex("[^a-z0-9]"), "").take(16).ifBlank { "fan" }
                val handle = "@$handleBase${uid.take(4)}".take(40)
                val profilePayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "handle" to handle,
                    "bio" to "Football & Cricket supporter on MONTANU SCORE",
                    "avatarUrl" to avatarUrl?.take(600),
                    "fanBadge" to "Founding Fan",
                    "verified" to false,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "onlineStatus" to "ONLINE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                profileRef.set(profilePayload).await()
            }

            val triviaRef = db.collection("trivia_scores").document(uid)
            if (!triviaRef.get().await().exists()) {
                triviaRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName.ifBlank { "Montanu Fan" }.take(60),
                        "points" to 0L,
                        "correctAnswers" to 0L,
                        "streak" to 0L,
                        "badge" to "Rookie",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$userPath / $profilePath")
            Result.failure(e)
        }
    }

    fun observeUserAccount(uid: String): Flow<UserAccount?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { it.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observePublicProfiles(): Flow<List<PublicProfile>> = flow {
        val path = "public_profiles"
        emitAll(
            db.collection(path)
                .whereGreaterThanOrEqualTo("followersCount", 0L)
                .snapshots()
                .map { it.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeFeedPosts(): Flow<List<SocialPost>> = flow {
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", "ACTIVE")
                .snapshots()
                .map { snap ->
                    snap.toObjects(SocialPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePostComments(postId: String): Flow<List<PostComment>> = flow {
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("status", "ACTIVE")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePublicGroups(): Flow<List<SportsGroup>> = flow {
        val path = "groups"
        emitAll(
            db.collection(path)
                .whereEqualTo("privacy", "PUBLIC")
                .snapshots()
                .map { it.toObjects(SportsGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMyGroups(uid: String): Flow<List<SportsGroup>> = flow {
        val path = "groups"
        emitAll(
            db.collection(path)
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { it.toObjects(SportsGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeGroupMessages(group: SportsGroup, uid: String): Flow<List<GroupMessage>> = flow {
        val path = "groups/${group.id}/messages"
        val query = if (group.privacy == "PUBLIC") {
            db.collection("groups").document(group.id).collection("messages")
                .whereEqualTo("privacy", "PUBLIC")
        } else {
            db.collection("groups").document(group.id).collection("messages")
                .whereArrayContains("memberUids", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(GroupMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeDirectChats(uid: String): Flow<List<DirectChat>> = flow {
        val path = "chats"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeChatMessages(chatId: String, uid: String): Flow<List<ChatMessage>> = flow {
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeBookmarks(uid: String): Flow<List<BookmarkItem>> = flow {
        val path = "users/$uid/bookmarks"
        emitAll(
            db.collection("users").document(uid).collection("bookmarks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(BookmarkItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeFollowing(uid: String): Flow<List<FollowLink>> = flow {
        val path = "users/$uid/following"
        emitAll(
            db.collection("users").document(uid).collection("following")
                .snapshots()
                .map { it.toObjects(FollowLink::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeFriendRequests(uid: String): Flow<List<FriendRequest>> = flow {
        val path = "users/$uid/friend_requests"
        emitAll(
            db.collection("users").document(uid).collection("friend_requests")
                .snapshots()
                .map { it.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeNotifications(uid: String): Flow<List<UserNotification>> = flow {
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
                }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<TriviaScore>> = flow {
        val path = "trivia_scores"
        emitAll(
            db.collection(path)
                .whereGreaterThanOrEqualTo("points", 0L)
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(25)
                .snapshots()
                .map { it.toObjects(TriviaScore::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAppConfig(): Flow<AppConfig?> = flow {
        val path = "app_config/current"
        emitAll(
            db.collection("app_config").document("current")
                .snapshots()
                .map { it.toObject(AppConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeReports(uid: String, isAdmin: Boolean): Flow<List<ModerationReport>> = flow {
        val path = "reports"
        val query = if (isAdmin) {
            db.collection(path)
        } else {
            db.collection(path).whereEqualTo("reporterId", uid)
        }
        emitAll(
            query.snapshots()
                .map { it.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    unwrapAndLogError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun createPost(
        authorName: String,
        authorAvatar: String?,
        authorVerified: Boolean,
        sportTag: String,
        mediaType: String,
        content: String,
        mediaUrl: String? = null,
        matchRefId: String? = null,
        matchLabel: String? = null,
        pollOptionA: String? = null,
        pollOptionB: String? = null
    ): Result<Unit> {
        val postId = "post_${UUID.randomUUID().toString().take(12)}"
        val path = "posts/$postId"
        return try {
            val uid = requireUserId()
            val trimmed = content.trim()
            require(trimmed.isNotEmpty() && trimmed.length <= 2000) { "Post must be 1 to 2000 characters." }
            val hashtags = Regex("#[A-Za-z0-9_]+").findAll(trimmed).map { it.value }.distinct().take(10).toList()
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to authorName.ifBlank { "Montanu Fan" }.take(60),
                "authorAvatar" to authorAvatar?.take(600),
                "authorVerified" to authorVerified,
                "sportTag" to sportTag,
                "mediaType" to mediaType,
                "content" to trimmed,
                "mediaUrl" to mediaUrl?.take(1000),
                "matchRefId" to matchRefId?.take(128),
                "matchLabel" to matchLabel?.take(160),
                "hashtags" to hashtags,
                "pollOptionA" to pollOptionA?.take(120),
                "pollOptionB" to pollOptionB?.take(120),
                "votesA" to 0L,
                "votesB" to 0L,
                "likesCount" to 0L,
                "commentsCount" to 0L,
                "sharesCount" to 0L,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikePost(postId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val likeRef = db.collection("posts").document(postId).collection("likes").document(uid)
            val postRef = db.collection("posts").document(postId)
            val snap = likeRef.get().await()
            if (snap.exists()) {
                likeRef.delete().await()
                postRef.update(
                    mapOf("likesCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            } else {
                likeRef.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
                postRef.update(
                    mapOf("likesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun votePoll(postId: String, optionA: Boolean): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            val field = if (optionA) "votesA" else "votesB"
            db.document(path).update(
                mapOf(field to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun incrementShare(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("sharesCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(postId: String, authorName: String, content: String, parentCommentId: String? = null): Result<Unit> {
        val commentId = "c_${UUID.randomUUID().toString().take(12)}"
        val path = "posts/$postId/comments/$commentId"
        return try {
            val uid = requireUserId()
            val trimmed = content.trim()
            require(trimmed.isNotEmpty() && trimmed.length <= 800) { "Comment must be 1 to 800 characters." }
            val payload = mapOf(
                "authorId" to uid,
                "authorName" to authorName.ifBlank { "Montanu Fan" }.take(60),
                "parentCommentId" to parentCommentId?.takeIf { it.isNotBlank() },
                "content" to trimmed,
                "status" to "ACTIVE",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            db.document(path).set(payload).await()
            db.collection("posts").document(postId).update(
                mapOf("commentsCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroup(name: String, description: String, sportTag: String, groupKind: String, privacy: String): Result<Unit> {
        val groupId = "grp_${UUID.randomUUID().toString().take(10)}"
        val path = "groups/$groupId"
        return try {
            val uid = requireUserId()
            require(name.trim().length in 2..80) { "Group name must be 2 to 80 characters." }
            val payload = mapOf(
                "name" to name.trim(),
                "description" to description.trim().take(400),
                "sportTag" to sportTag,
                "groupKind" to groupKind,
                "privacy" to privacy,
                "ownerId" to uid,
                "memberUids" to listOf(uid),
                "memberCount" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinPublicGroup(group: SportsGroup): Result<Unit> {
        val path = "groups/${group.id}"
        return try {
            val uid = requireUserId()
            if (uid !in group.memberUids) {
                db.document(path).update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "memberCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sendGroupMessage(group: SportsGroup, senderName: String, text: String): Result<Unit> {
        val msgId = "gm_${UUID.randomUUID().toString().take(12)}"
        val path = "groups/${group.id}/messages/$msgId"
        return try {
            val uid = requireUserId()
            val trimmed = text.trim()
            require(trimmed.isNotEmpty() && trimmed.length <= 1000) { "Message cannot be empty." }
            val payload = mapOf(
                "senderId" to uid,
                "senderName" to senderName.ifBlank { "Fan" }.take(60),
                "text" to trimmed,
                "privacy" to group.privacy,
                "memberUids" to group.memberUids.take(50),
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun startOrSendDirectChat(
        otherUid: String,
        otherName: String,
        myName: String,
        messageText: String
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val sorted = listOf(uid, otherUid).sorted()
            val chatId = "chat_${sorted[0].take(16)}_${sorted[1].take(16)}"
            val chatRef = db.collection("chats").document(chatId)
            val trimmed = messageText.trim().take(1000)
            if (!chatRef.get().await().exists()) {
                chatRef.set(
                    mapOf(
                        "participantUids" to sorted,
                        "participantNames" to listOf(myName.take(60), otherName.take(60)),
                        "lastMessage" to trimmed.ifBlank { "Chat started" },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (trimmed.isNotBlank()) {
                chatRef.update(
                    mapOf("lastMessage" to trimmed, "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            if (trimmed.isNotBlank()) {
                val msgId = "m_${UUID.randomUUID().toString().take(12)}"
                chatRef.collection("messages").document(msgId).set(
                    mapOf(
                        "senderId" to uid,
                        "senderName" to myName.take(60),
                        "text" to trimmed,
                        "participantUids" to sorted,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "chats")
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(target: PublicProfile, currentlyFollowing: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val followRef = db.collection("users").document(uid).collection("following").document(target.userId)
            val targetProfileRef = db.collection("public_profiles").document(target.userId)
            if (currentlyFollowing) {
                followRef.delete().await()
                targetProfileRef.update(
                    mapOf("followersCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            } else {
                followRef.set(
                    mapOf(
                        "targetUid" to target.userId,
                        "targetName" to target.displayName.take(60),
                        "targetAvatar" to target.avatarUrl,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                ).await()
                targetProfileRef.update(
                    mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "following/${target.userId}")
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(targetUid: String, myName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("users").document(targetUid).collection("friend_requests").document(uid).set(
                mapOf(
                    "requesterUid" to uid,
                    "requesterName" to myName.take(60),
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$targetUid/friend_requests")
            Result.failure(e)
        }
    }

    suspend fun respondFriendRequest(requesterUid: String, accept: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            val newStatus = if (accept) "ACCEPTED" else "DECLINED"
            db.collection("users").document(uid).collection("friend_requests").document(requesterUid)
                .update(mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "friend_requests/$requesterUid")
            Result.failure(e)
        }
    }

    suspend fun saveBookmark(
        bookmarkType: String,
        itemId: String,
        sport: String,
        title: String,
        subtitle: String,
        payloadJson: String? = null
    ): Result<Unit> {
        val safeDocId = "${bookmarkType}_${itemId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("bookmarks").document(safeDocId)
            val data = mapOf(
                "bookmarkType" to bookmarkType,
                "itemId" to itemId.take(128),
                "sport" to sport,
                "title" to title.take(160),
                "subtitle" to subtitle.take(300),
                "payloadJson" to payloadJson?.take(8000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(data).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "bookmarks/$safeDocId")
            Result.failure(e)
        }
    }

    suspend fun removeBookmark(bookmarkDocId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).collection("bookmarks").document(bookmarkDocId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "bookmarks/$bookmarkDocId")
            Result.failure(e)
        }
    }

    suspend fun reportTarget(targetType: String, targetId: String, reason: String): Result<Unit> {
        val repId = "rep_${UUID.randomUUID().toString().take(12)}"
        return try {
            val uid = requireUserId()
            require(reason.trim().length >= 3) { "Please provide a reason (at least 3 chars)." }
            db.collection("reports").document(repId).set(
                mapOf(
                    "reporterId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId.take(128),
                    "reason" to reason.trim().take(500),
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reports/$repId")
            Result.failure(e)
        }
    }

    suspend fun blockUser(blockedUid: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).update(
                mapOf(
                    "blockedUserIds" to FieldValue.arrayUnion(blockedUid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/block")
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        darkMode: Boolean? = null,
        languageCode: String? = null,
        highContrast: Boolean? = null,
        matchRemindersEnabled: Boolean? = null,
        favTeamToggle: String? = null,
        favPlayerToggle: String? = null,
        currentFavTeams: List<String> = emptyList(),
        currentFavPlayers: List<String> = emptyList()
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            darkMode?.let { updates["darkMode"] = it }
            languageCode?.let { updates["languageCode"] = it }
            highContrast?.let { updates["highContrast"] = it }
            matchRemindersEnabled?.let { updates["matchRemindersEnabled"] = it }
            favTeamToggle?.let { team ->
                val next = if (team in currentFavTeams) currentFavTeams - team else (currentFavTeams + team).take(30)
                updates["favTeamIds"] = next
            }
            favPlayerToggle?.let { player ->
                val next = if (player in currentFavPlayers) currentFavPlayers - player else (currentFavPlayers + player).take(30)
                updates["favPlayerIds"] = next
            }
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/prefs")
            Result.failure(e)
        }
    }

    suspend fun updatePublicProfile(displayName: String, bio: String, fanBadge: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val safeName = displayName.trim().take(60).ifBlank { "Montanu Fan" }
            db.collection("public_profiles").document(uid).update(
                mapOf(
                    "displayName" to safeName,
                    "bio" to bio.trim().take(300),
                    "fanBadge" to fanBadge.trim().take(40).ifBlank { "Fan" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).update(
                mapOf("displayName" to safeName, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "public_profiles")
            Result.failure(e)
        }
    }

    suspend fun recordTriviaResult(displayName: String, earnedPoints: Long, correctCount: Long): Result<Unit> {
        return try {
            val uid = requireUserId()
            val badge = when {
                earnedPoints >= 100 -> "Hall of Fame"
                earnedPoints >= 50 -> "Tactician"
                else -> "Challenger"
            }
            db.collection("users").document(uid).update(
                mapOf(
                    "triviaPoints" to FieldValue.increment(earnedPoints),
                    "triviaStreak" to FieldValue.increment(1L),
                    "achievements" to FieldValue.arrayUnion("TRIVIA_PLAYER"),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("trivia_scores").document(uid).update(
                mapOf(
                    "displayName" to displayName.take(60),
                    "points" to FieldValue.increment(earnedPoints),
                    "correctAnswers" to FieldValue.increment(correctCount),
                    "streak" to FieldValue.increment(1L),
                    "badge" to badge,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "trivia_scores/${auth.currentUser?.uid ?: "unknown"}")
            Result.failure(e)
        }
    }

    suspend fun submitFeedback(subject: String, message: String): Result<Unit> {
        val fbId = "fb_${UUID.randomUUID().toString().take(12)}"
        return try {
            val uid = requireUserId()
            require(subject.trim().length >= 2 && message.trim().length >= 5) {
                "Please provide a subject and detailed feedback."
            }
            db.collection("feedback").document(fbId).set(
                mapOf(
                    "userId" to uid,
                    "subject" to subject.trim().take(120),
                    "message" to message.trim().take(1500),
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "feedback/$fbId")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("public_profiles").document(uid).delete().await()
            db.collection("trivia_scores").document(uid).delete().await()
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/deleteAccount")
            Result.failure(e)
        }
    }

    // --- ADMIN OPERATIONS ---

    suspend fun adminPublishUpdateConfig(
        versionCode: Long,
        versionName: String,
        mandatory: Boolean,
        maintenance: Boolean,
        whatsNew: String,
        title: String,
        body: String
    ): Result<Unit> {
        val path = "app_config/current"
        return try {
            requireUserId()
            val ref = db.document(path)
            if (ref.get().await().exists()) {
                ref.update(
                    mapOf(
                        "latestVersionCode" to versionCode,
                        "latestVersionName" to versionName.take(30),
                        "mandatoryUpdate" to mandatory,
                        "maintenanceMode" to maintenance,
                        "whatsNewNotes" to whatsNew.take(1500),
                        "announcementTitle" to title.take(120),
                        "announcementBody" to body.take(1000),
                        "playStoreUrl" to "https://play.google.com/store/apps/details?id=com.aistudio.montanuscore.vxqrmt",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "latestVersionCode" to versionCode,
                        "latestVersionName" to versionName.take(30),
                        "mandatoryUpdate" to mandatory,
                        "maintenanceMode" to maintenance,
                        "whatsNewNotes" to whatsNew.take(1500),
                        "announcementTitle" to title.take(120),
                        "announcementBody" to body.take(1000),
                        "playStoreUrl" to "https://play.google.com/store/apps/details?id=com.aistudio.montanuscore.vxqrmt",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminModeratePost(postId: String, newStatus: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminResolveReport(reportId: String, newStatus: String): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminToggleVerifyUser(targetUid: String, verified: Boolean): Result<Unit> {
        val path = "public_profiles/$targetUid"
        return try {
            requireUserId()
            db.document(path).update(
                mapOf("verified" to verified, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
