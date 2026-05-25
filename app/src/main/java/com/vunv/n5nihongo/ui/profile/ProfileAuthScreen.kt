package com.vunv.n5nihongo.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileAuthRoute(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    ProfileAuthScreen(
        uiState = uiState,
        onEmailChange = authViewModel::onEmailChange,
        onPasswordChange = authViewModel::onPasswordChange,
        onDisplayNameChange = authViewModel::onDisplayNameChange,
        onSubmit = authViewModel::submitAuth,
        onToggleMode = authViewModel::toggleMode,
        onLogout = authViewModel::logout,
        modifier = modifier
    )
}

@Composable
private fun ProfileAuthScreen(
    uiState: ProfileUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.currentUser != null) {
        LoggedInProfile(
            uiState = uiState,
            onLogout = onLogout,
            modifier = modifier
        )
        return
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (uiState.isLoginMode) "Đăng nhập" else "Đăng ký",
                style = MaterialTheme.typography.headlineMedium
            )
            if (!uiState.isLoginMode) {
                OutlinedTextField(
                    value = uiState.displayNameInput,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Tên hiển thị") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = { Text("Mật khẩu") },
                modifier = Modifier.fillMaxWidth()
            )

            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Button(
                onClick = onSubmit,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isLoginMode) "Đăng nhập" else "Tạo tài khoản")
            }
            OutlinedButton(onClick = onToggleMode, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (uiState.isLoginMode) {
                        "Chưa có tài khoản? Đăng ký"
                    } else {
                        "Đã có tài khoản? Đăng nhập"
                    }
                )
            }
        }
    }
}

@Composable
private fun LoggedInProfile(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = uiState.currentUser ?: return
    val userDocument = uiState.userDocument

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Hồ sơ của bạn", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = user.displayName ?: userDocument?.displayName ?: "Người dùng",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "Email: ${user.email ?: "Không có"}")
            Text(text = "UID: ${user.uid}")
            Text(text = "Tổng XP: ${userDocument?.totalXp ?: 0}")
            Text(text = "Chuỗi ngày: ${userDocument?.streak ?: 0}")

            uiState.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Đăng xuất")
            }
        }
    }
}
