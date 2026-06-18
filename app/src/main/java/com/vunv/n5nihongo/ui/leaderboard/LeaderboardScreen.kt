package com.vunv.n5nihongo.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vunv.n5nihongo.data.auth.UserDocument
import com.vunv.n5nihongo.ui.theme.MintLight
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SurfaceWhite
import com.vunv.n5nihongo.ui.theme.TextPrimary
import com.vunv.n5nihongo.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onStartMockExam: () -> Unit,
    viewModel: LeaderboardViewModel = viewModel(factory = LeaderboardViewModel.factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadLeaderboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bảng Xếp Hạng N5 🏆",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadLeaderboard() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Tải lại",
                            tint = MintPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceWhite,
                            MintLight.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MintPrimary)
                    }
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Đã có lỗi xảy ra",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadLeaderboard() },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
                else -> {
                    val users = uiState.topUsers
                    val podiumUsers = users.take(3)
                    val listUsers = if (users.size > 3) users.subList(3, users.size) else emptyList()

                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            // 1. Podium Section
                            item {
                                if (podiumUsers.isNotEmpty()) {
                                    PodiumSection(podiumUsers = podiumUsers)
                                }
                            }

                            // 2. Rank List Header
                            if (listUsers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Top Cao Thủ Khác",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }

                            // 3. Rank List Items (Rank 4+)
                            itemsIndexed(listUsers) { index, user ->
                                LeaderboardRow(
                                    rank = index + 4,
                                    user = user
                                )
                            }

                            if (users.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Chưa có cao thủ nào ghi danh!", color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Sticky Bottom User Stats Card
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        BottomUserBar(
                            currentUserDoc = uiState.currentUserDoc,
                            rank = uiState.currentUserRank,
                            onStartMockExam = onStartMockExam
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(podiumUsers: List<UserDocument>) {
    val second = podiumUsers.getOrNull(1)
    val first = podiumUsers.getOrNull(0)
    val third = podiumUsers.getOrNull(2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Rank 2 (Left)
        Box(modifier = Modifier.weight(1f)) {
            PodiumColumn(
                user = second,
                rank = 2,
                height = 120.dp,
                medalColor = Color(0xFFC0C0C0),
                podiumText = "🥈"
            )
        }

        // Rank 1 (Middle - Taller)
        Box(modifier = Modifier.weight(1.1f)) {
            PodiumColumn(
                user = first,
                rank = 1,
                height = 150.dp,
                medalColor = Color(0xFFFFD700),
                podiumText = "👑",
                isFirst = true
            )
        }

        // Rank 3 (Right)
        Box(modifier = Modifier.weight(1f)) {
            PodiumColumn(
                user = third,
                rank = 3,
                height = 100.dp,
                medalColor = Color(0xFFCD7F32),
                podiumText = "🥉"
            )
        }
    }
}

@Composable
private fun PodiumColumn(
    user: UserDocument?,
    rank: Int,
    height: androidx.compose.ui.unit.Dp,
    medalColor: Color,
    podiumText: String,
    isFirst: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user != null) {
            // Crown/Podium Icon
            Text(text = podiumText, fontSize = if (isFirst) 34.sp else 26.sp)
            Spacer(modifier = Modifier.height(4.dp))

            // Avatar Container with Border
            Box(
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(
                    photoUrl = user.photoUrl,
                    displayName = user.displayName,
                    size = if (isFirst) 72.dp else 60.dp,
                    borderColor = medalColor,
                    borderWidth = if (isFirst) 3.dp else 2.dp
                )
                // Number Badge
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(medalColor)
                        .align(Alignment.BottomCenter)
                        .border(1.dp, SurfaceWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        fontSize = 11.sp,
                        color = if (isFirst) TextPrimary else SurfaceWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Name
            Text(
                text = user.displayName.ifBlank { "Ẩn Danh" },
                fontWeight = FontWeight.Bold,
                fontSize = if (isFirst) 14.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // XP display
            Text(
                text = "${user.totalXp} XP",
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (isFirst) 13.sp else 11.sp,
                color = MintPrimary
            )

            // Streak indicator
            if (user.streak > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFF97316),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${user.streak}d",
                        fontSize = 10.sp,
                        color = Color(0xFFF97316),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Placeholder empty column
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MintLight.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("-", color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Podium Pedestal Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            medalColor.copy(alpha = 0.45f),
                            medalColor.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = medalColor.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
        )
    }
}

@Composable
private fun LeaderboardRow(rank: Int, user: UserDocument) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Text(
                text = "#$rank",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextSecondary,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Start
            )

            // User Avatar
            AvatarImage(
                photoUrl = user.photoUrl,
                displayName = user.displayName,
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName.ifBlank { "Học viên ẩn danh" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.streak > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${user.streak} ngày học liên tiếp",
                            fontSize = 11.sp,
                            color = Color(0xFFF97316),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // User XP
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "XP",
                    tint = MintPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${user.totalXp} XP",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MintPrimary
                )
            }
        }
    }
}

@Composable
private fun BottomUserBar(
    currentUserDoc: UserDocument?,
    rank: Int,
    onStartMockExam: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentUserDoc != null) {
                // User Rank
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = if (rank > 0) "#$rank" else "-",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MintPrimary
                    )
                    Text(
                        text = "Hạng",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                // Avatar
                AvatarImage(
                    photoUrl = currentUserDoc.photoUrl,
                    displayName = currentUserDoc.displayName,
                    size = 44.dp,
                    borderColor = MintPrimary,
                    borderWidth = 1.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Name & Streak
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bạn (${currentUserDoc.displayName.ifBlank { "Tôi" }})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${currentUserDoc.streak}d streak",
                            fontSize = 12.sp,
                            color = Color(0xFFF97316),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${currentUserDoc.totalXp} XP",
                            fontSize = 12.sp,
                            color = MintPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "Hãy đăng nhập để tham gia bảng xếp hạng!",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Challenge Button
            Button(
                onClick = onStartMockExam,
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Thi",
                    tint = SurfaceWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Thi thử",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AvatarImage(
    photoUrl: String,
    displayName: String,
    size: androidx.compose.ui.unit.Dp,
    borderColor: Color = Color.Transparent,
    borderWidth: androidx.compose.ui.unit.Dp = 0.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MintLight.copy(alpha = 0.4f))
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNotBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val initial = displayName.trim().firstOrNull()?.toString()?.uppercase() ?: "U"
            Text(
                text = initial,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp,
                color = MintPrimary
            )
        }
    }
}
