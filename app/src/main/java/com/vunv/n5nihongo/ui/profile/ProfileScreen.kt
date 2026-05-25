package com.vunv.n5nihongo.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.vunv.n5nihongo.ui.theme.LightBackground
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SkySecondary
import androidx.compose.material3.HorizontalDivider

private data class BadgeUi(val title: String, val achieved: Boolean)

@Composable
fun ProfileRoute(
    onLogoutSuccess: () -> Unit,
    onNavigateToAi: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val userName = uiState.userDocument?.displayName?.takeIf { it.isNotBlank() } ?: uiState.currentUser?.displayName ?: "Bạn học Nihongo"
    val photoUrl = uiState.userDocument?.photoUrl?.takeIf { it.isNotBlank() } ?: uiState.currentUser?.photoUrl?.toString().orEmpty()
    val xp = uiState.userDocument?.totalXp ?: 0
    val streak = uiState.userDocument?.streak ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileHeader(userName = userName, photoUrl = photoUrl)
        StatsRow(streak = streak, xp = xp, rank = "Đồng")
        AchievementSection(
            badges = listOf(
                BadgeUi("Tân binh Nihongo", xp >= 50),
                BadgeUi("Chăm chỉ", streak >= 3),
                BadgeUi("Bậc thầy Từ vựng", xp >= 500),
                BadgeUi("Huyền thoại N5", xp >= 2000)
            )
        )
        SettingsList(
            onNavigateToAi = onNavigateToAi,
            onLogout = {
                authViewModel.logout()
                onLogoutSuccess()
            }
        )
    }
}

@Composable
private fun ProfileHeader(userName: String, photoUrl: String) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MintPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "👤",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MintPrimary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Trình độ N5", color = SkySecondary)
            }
        }
    }
}

@Composable
private fun StatsRow(streak: Int, xp: Int, rank: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("Chuỗi ngày", "$streak", Icons.Default.LocalFireDepartment, Modifier.weight(1f))
        StatCard("XP", "$xp", Icons.Default.Star, Modifier.weight(1f))
        StatCard("Xếp hạng", rank, Icons.Default.EmojiEvents, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MintPrimary)
            Text(title, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AchievementSection(badges: List<BadgeUi>) {
    Text("Huy hiệu", style = MaterialTheme.typography.titleLarge)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        items(badges) { badge ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .alpha(if (badge.achieved) 1f else 0.5f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = if (badge.achieved) SkySecondary else Color.Gray)
                    Text(badge.title)
                }
            }
        }
    }
}

@Composable
private fun SettingsList(onNavigateToAi: () -> Unit, onLogout: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            SettingRow("Trợ lý AI Sensei 🌸", onClick = onNavigateToAi)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingRow("Đăng xuất", onClick = onLogout)
        }
    }
}

@Composable
private fun SettingRow(label: String, onClick: () -> Unit = {}) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null)
        }
    }
}
