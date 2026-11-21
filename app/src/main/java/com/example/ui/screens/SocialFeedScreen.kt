package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.DirectChat
import com.example.data.model.FollowLink
import com.example.data.model.FriendRequest
import com.example.data.model.GroupMessage
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.SocialPost
import com.example.data.model.SportsGroup
import com.example.data.model.UiState
import com.example.i18n.AppStrings
import com.example.ui.components.shareTextCard
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.viewmodel.SocialHubViewModel

private enum class SocialTab { FEED, TRENDING, GROUPS, MESSAGES, FRIENDS }

@Composable
fun SocialFeedScreen(
    vm: SocialHubViewModel,
    strings: AppStrings,
    feedState: UiState<List<SocialPost>>,
    profilesState: UiState<List<PublicProfile>>,
    publicGroupsState: UiState<List<SportsGroup>>,
    myGroupsState: UiState<List<SportsGroup>>,
    chatsState: UiState<List<DirectChat>>,
    followingState: UiState<List<FollowLink>>,
    friendRequestsState: UiState<List<FriendRequest>>,
    postCommentsState: UiState<List<PostComment>>,
    groupMessagesState: UiState<List<GroupMessage>>,
    chatMessagesState: UiState<List<ChatMessage>>,
    blockedUids: List<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(SocialTab.FEED) }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var commentPostTarget by remember { mutableStateOf<SocialPost?>(null) }
    var reportTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var activeGroup by remember { mutableStateOf<SportsGroup?>(null) }
    var activeChat by remember { mutableStateOf<DirectChat?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(strings.navSocial, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        SocialTab.FEED to strings.feed,
                        SocialTab.TRENDING to strings.trending,
                        SocialTab.GROUPS to strings.groups,
                        SocialTab.MESSAGES to strings.messages,
                        SocialTab.FRIENDS to strings.friends
                    ).forEach { (tab, label) ->
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            label = { Text(label) },
                            modifier = Modifier.testTag("social_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            when (activeTab) {
                SocialTab.FEED, SocialTab.TRENDING -> {
                    when (feedState) {
                        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        is UiState.Error -> LiquidGlassCard(Modifier.padding(16.dp).fillMaxWidth()) { Text(feedState.message, color = MaterialTheme.colorScheme.error) }
                        is UiState.Success -> {
                            val posts = feedState.data
                                .filter { it.authorId !in blockedUids }
                                .let { list ->
                                    if (activeTab == SocialTab.TRENDING) {
                                        list.sortedByDescending { it.likesCount + it.commentsCount + it.sharesCount }
                                    } else list
                                }
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (posts.isEmpty()) {
                                    item {
                                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                            Text("No posts yet. Tap + to publish match thoughts, polls, or predictions!", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                } else {
                                    items(posts, key = { it.id }) { post ->
                                        SocialPostCard(
                                            post = post,
                                            strings = strings,
                                            isOwnPost = post.authorId == vm.currentUserId,
                                            onLike = { vm.toggleLike(post.id) },
                                            onVoteA = { vm.votePoll(post.id, true) },
                                            onVoteB = { vm.votePoll(post.id, false) },
                                            onComment = {
                                                commentPostTarget = post
                                                vm.selectPostForComments(post.id)
                                            },
                                            onShare = {
                                                vm.sharePost(post.id)
                                                shareTextCard(context, "${post.authorName} on MONTANU SCORE", post.content)
                                            },
                                            onSave = {
                                                vm.saveBookmark("POST", post.id, post.sportTag, post.authorName, post.content.take(120))
                                            },
                                            onReport = { reportTarget = "POST" to post.id },
                                            onBlock = { vm.blockUser(post.authorId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SocialTab.GROUPS -> {
                    if (activeGroup != null) {
                        GroupChatRoomView(
                            group = activeGroup!!,
                            messagesState = groupMessagesState,
                            strings = strings,
                            onSend = { text -> vm.sendGroupMessage(activeGroup!!, text) },
                            onBack = {
                                activeGroup = null
                                vm.selectGroup(null)
                            }
                        )
                    } else {
                        val pub = (publicGroupsState as? UiState.Success)?.data.orEmpty()
                        val mine = (myGroupsState as? UiState.Success)?.data.orEmpty()
                        val merged = (mine + pub).distinctBy { it.id }

                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Button(
                                    onClick = { showCreateGroupDialog = true },
                                    modifier = Modifier.fillMaxWidth().testTag("create_group_btn")
                                ) {
                                    Icon(Icons.Filled.GroupAdd, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.createGroup)
                                }
                            }
                            items(merged, key = { it.id }) { grp ->
                                val isMember = vm.currentUserId in grp.memberUids
                                LiquidGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (isMember || grp.privacy == "PUBLIC") {
                                            activeGroup = grp
                                            vm.selectGroup(grp)
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (grp.privacy == "PUBLIC") Icons.Filled.Public else Icons.Filled.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(grp.name, style = MaterialTheme.typography.titleMedium)
                                            }
                                            Text("${grp.sportTag} • ${grp.groupKind} • ${grp.memberCount} members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                            if (grp.description.isNotBlank()) {
                                                Text(grp.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (!isMember && grp.privacy == "PUBLIC") {
                                            Button(onClick = { vm.joinGroup(grp) }) { Text(strings.joinGroup) }
                                        } else {
                                            Text("Open", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SocialTab.MESSAGES -> {
                    if (activeChat != null) {
                        DirectChatRoomView(
                            chat = activeChat!!,
                            myUid = vm.currentUserId,
                            messagesState = chatMessagesState,
                            strings = strings,
                            onSend = { text ->
                                val otherUid = activeChat!!.participantUids.firstOrNull { it != vm.currentUserId } ?: vm.currentUserId
                                val otherName = activeChat!!.participantNames.firstOrNull() ?: "Fan"
                                vm.sendDirectMessage(otherUid, otherName, text)
                            },
                            onBack = {
                                activeChat = null
                                vm.selectChat(null)
                            }
                        )
                    } else {
                        val chats = (chatsState as? UiState.Success)?.data.orEmpty()
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (chats.isEmpty()) {
                                item {
                                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                        Text("No direct chats yet. Visit the Friends tab to message any fan!", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            } else {
                                items(chats, key = { it.id }) { chat ->
                                    LiquidGlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            activeChat = chat
                                            vm.selectChat(chat.id)
                                        }
                                    ) {
                                        Column {
                                            Text(chat.participantNames.joinToString(" & "), style = MaterialTheme.typography.titleMedium)
                                            Text(chat.lastMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SocialTab.FRIENDS -> {
                    val profiles = (profilesState as? UiState.Success)?.data.orEmpty()
                        .filter { it.userId != vm.currentUserId && it.userId !in blockedUids }
                    val followingSet = (followingState as? UiState.Success)?.data.orEmpty().map { it.targetUid }.toSet()
                    val requests = (friendRequestsState as? UiState.Success)?.data.orEmpty().filter { it.status == "PENDING" }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (requests.isNotEmpty()) {
                            item { Text("Friend Requests", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                            items(requests, key = { it.id }) { req ->
                                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(req.requesterName, style = MaterialTheme.typography.titleMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { vm.respondFriendRequest(req.requesterUid, true) }) { Text("Accept") }
                                            OutlinedButton(onClick = { vm.respondFriendRequest(req.requesterUid, false) }) { Text("Decline") }
                                        }
                                    }
                                }
                            }
                        }

                        item { Text("Sports Community Directory", style = MaterialTheme.typography.titleMedium) }
                        items(profiles, key = { it.userId }) { profile ->
                            val isFollowing = profile.userId in followingSet
                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(profile.displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                                                if (profile.verified) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text("${profile.handle} • ${profile.fanBadge} • ${profile.onlineStatus}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { vm.toggleFollow(profile, isFollowing) }) {
                                            Text(if (isFollowing) strings.following else strings.follow)
                                        }
                                        OutlinedButton(onClick = { vm.sendFriendRequest(profile.userId) }) {
                                            Text("Add Friend")
                                        }
                                        OutlinedButton(onClick = {
                                            vm.sendDirectMessage(profile.userId, profile.displayName, "Hey ${profile.displayName}!")
                                            activeTab = SocialTab.MESSAGES
                                        }) {
                                            Text("Message")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeTab == SocialTab.FEED || activeTab == SocialTab.TRENDING) {
            FloatingActionButton(
                onClick = { showCreatePostDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 96.dp)
                    .testTag("create_post_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.createPost)
            }
        }
    }

    if (showCreatePostDialog) {
        CreatePostDialog(
            strings = strings,
            onDismiss = { showCreatePostDialog = false },
            onSubmit = { sportTag, mediaType, content, mediaUrl, optA, optB ->
                vm.createPost(sportTag, mediaType, content, mediaUrl, null, null, optA, optB)
                showCreatePostDialog = false
            }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            strings = strings,
            onDismiss = { showCreateGroupDialog = false },
            onSubmit = { name, desc, sport, kind, privacy ->
                vm.createGroup(name, desc, sport, kind, privacy)
                showCreateGroupDialog = false
            }
        )
    }

    if (commentPostTarget != null) {
        CommentsDialog(
            post = commentPostTarget!!,
            commentsState = postCommentsState,
            strings = strings,
            onDismiss = {
                commentPostTarget = null
                vm.selectPostForComments(null)
            },
            onSendComment = { text, parentId ->
                vm.addComment(commentPostTarget!!.id, text, parentId)
            }
        )
    }

    if (reportTarget != null) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text(strings.report) },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (spam, abuse, misinformation)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.reportContent(reportTarget!!.first, reportTarget!!.second, reason)
                    reportTarget = null
                }) { Text(strings.submit) }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun SocialPostCard(
    post: SocialPost,
    strings: AppStrings,
    isOwnPost: Boolean,
    onLike: () -> Unit,
    onVoteA: () -> Unit,
    onVoteB: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.authorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (post.authorVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Filled.Verified, contentDescription = strings.verifiedFan, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text("${post.sportTag} • ${post.mediaType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Row {
                    IconButton(onClick = onSave, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = strings.save, modifier = Modifier.size(18.dp))
                    }
                    if (!isOwnPost) {
                        IconButton(onClick = onReport, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.Flag, contentDescription = strings.report, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onBlock, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Filled.Block, contentDescription = strings.blockUser, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Text(post.content, style = MaterialTheme.typography.bodyLarge)

            if (!post.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = "Post media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            if ((post.mediaType == "POLL" || post.mediaType == "PREDICTION") && !post.pollOptionA.isNullOrBlank() && !post.pollOptionB.isNullOrBlank()) {
                val total = (post.votesA + post.votesB).coerceAtLeast(1L).toFloat()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onVoteA, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(post.pollOptionA)
                            Text("${post.votesA}")
                        }
                    }
                    LinearProgressIndicator(progress = { post.votesA / total }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)))
                    OutlinedButton(onClick = onVoteB, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(post.pollOptionB)
                            Text("${post.votesB}")
                        }
                    }
                    LinearProgressIndicator(progress = { post.votesB / total }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onLike) {
                    Icon(Icons.Filled.Favorite, contentDescription = strings.like, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${post.likesCount}")
                }
                TextButton(onClick = onComment) {
                    Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = strings.comment, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${post.commentsCount}")
                }
                TextButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = strings.share, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${post.sharesCount}")
                }
            }
        }
    }
}

@Composable
private fun CreatePostDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String?, String?, String?) -> Unit
) {
    var sportTag by remember { mutableStateOf("FOOTBALL") }
    var mediaType by remember { mutableStateOf("TEXT") }
    var content by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pollA by remember { mutableStateOf("") }
    var pollB by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pickedUri = uri
            mediaType = "IMAGE"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.createPost) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = sportTag == "FOOTBALL", onClick = { sportTag = "FOOTBALL" }, label = { Text("Football") })
                    FilterChip(selected = sportTag == "CRICKET", onClick = { sportTag = "CRICKET" }, label = { Text("Cricket") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mediaType == "TEXT", onClick = { mediaType = "TEXT" }, label = { Text("Text") })
                    FilterChip(selected = mediaType == "POLL", onClick = { mediaType = "POLL" }, label = { Text("Poll") })
                    FilterChip(selected = mediaType == "PREDICTION", onClick = { mediaType = "PREDICTION" }, label = { Text("Predict") })
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Share match takes, #hashtags or @mentions") },
                    modifier = Modifier.fillMaxWidth().testTag("post_content_input")
                )
                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (pickedUri != null) "Image Selected" else "Attach Photo (Photo Picker)")
                }
                if (mediaType == "POLL" || mediaType == "PREDICTION") {
                    OutlinedTextField(value = pollA, onValueChange = { pollA = it }, label = { Text("Option A") }, singleLine = true)
                    OutlinedTextField(value = pollB, onValueChange = { pollB = it }, label = { Text("Option B") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        sportTag,
                        mediaType,
                        content,
                        pickedUri?.toString(),
                        pollA.takeIf { it.isNotBlank() },
                        pollB.takeIf { it.isNotBlank() }
                    )
                },
                enabled = content.isNotBlank()
            ) { Text(strings.submit) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun CreateGroupDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("FOOTBALL") }
    var kind by remember { mutableStateOf("TEAM_CLUB") }
    var privacy by remember { mutableStateOf("PUBLIC") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.createGroup) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Community / Fan Club Name") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = sport == "FOOTBALL", onClick = { sport = "FOOTBALL" }, label = { Text("Football") })
                    FilterChip(selected = sport == "CRICKET", onClick = { sport = "CRICKET" }, label = { Text("Cricket") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = privacy == "PUBLIC", onClick = { privacy = "PUBLIC" }, label = { Text(strings.publicGroup) })
                    FilterChip(selected = privacy == "PRIVATE", onClick = { privacy = "PRIVATE" }, label = { Text(strings.privateGroup) })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(name, desc, sport, kind, privacy) }, enabled = name.trim().length >= 2) {
                Text(strings.submit)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun CommentsDialog(
    post: SocialPost,
    commentsState: UiState<List<PostComment>>,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSendComment: (String, String?) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var replyToId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${post.authorName} — ${strings.comment}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val comments = (commentsState as? UiState.Success)?.data.orEmpty()
                if (comments.isEmpty()) {
                    Text("No comments yet. Start the discussion!", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(comments, key = { it.id }) { c ->
                            LiquidGlassCard(contentPadding = PaddingValues(8.dp)) {
                                Column {
                                    Text(
                                        text = if (c.parentCommentId != null) "↳ ${c.authorName}" else c.authorName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(c.content, style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = { replyToId = c.id }) { Text("Reply") }
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text(if (replyToId != null) "Replying to comment..." else strings.writeComment) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSendComment(draft, replyToId)
                    draft = ""
                    replyToId = null
                },
                enabled = draft.isNotBlank()
            ) { Text(strings.send) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun GroupChatRoomView(
    group: SportsGroup,
    messagesState: UiState<List<GroupMessage>>,
    strings: AppStrings,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(group.name, style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("Leave Room") }
        }
        val msgs = (messagesState as? UiState.Success)?.data.orEmpty()
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs, key = { it.id }) { m ->
                LiquidGlassCard(contentPadding = PaddingValues(10.dp)) {
                    Column {
                        Text(m.senderName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(m.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 96.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Message ${group.name}") }, modifier = Modifier.weight(1f), singleLine = true)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { if (text.isNotBlank()) { onSend(text); text = "" } }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.send, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DirectChatRoomView(
    chat: DirectChat,
    myUid: String,
    messagesState: UiState<List<ChatMessage>>,
    strings: AppStrings,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(chat.participantNames.joinToString(" & "), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("Back") }
        }
        val msgs = (messagesState as? UiState.Success)?.data.orEmpty()
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs, key = { it.id }) { m ->
                LiquidGlassCard(
                    highlightBorder = m.senderId == myUid,
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Column {
                        Text(m.senderName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(m.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 96.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Write direct message") }, modifier = Modifier.weight(1f), singleLine = true)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { if (text.isNotBlank()) { onSend(text); text = "" } }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = strings.send, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
