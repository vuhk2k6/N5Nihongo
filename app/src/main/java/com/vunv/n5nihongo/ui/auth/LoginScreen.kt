package com.vunv.n5nihongo.ui.auth

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.R
import com.vunv.n5nihongo.data.auth.FacebookAuthCallbacks
import com.vunv.n5nihongo.ui.profile.AuthViewModel
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SkySecondary

@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val callbackManager = remember { CallbackManager.Factory.create() }
    val loginManager = remember { runCatching { LoginManager.getInstance() }.getOrNull() }
    val credentialManager = remember { CredentialManager.create(context) }

    DisposableEffect(callbackManager) {
        FacebookAuthCallbacks.callbackManager = callbackManager
        onDispose {
            if (FacebookAuthCallbacks.callbackManager === callbackManager) {
                FacebookAuthCallbacks.callbackManager = null
            }
        }
    }

    // Removed legacy GoogleSignIn activity launcher

    LaunchedEffect(Unit) {
        if (loginManager == null) {
            return@LaunchedEffect
        }
        runCatching {
            loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    authViewModel.loginWithFacebookToken(result.accessToken.token)
                }

                override fun onCancel() {
                    authViewModel.showAuthMessage("Đăng nhập Facebook đã hủy")
                }

                override fun onError(error: FacebookException) {
                    Log.e("LoginScreen", "Facebook login error: ${error.message}", error)
                    authViewModel.showAuthMessage(error.message ?: "Đăng nhập Facebook thất bại")
                }
            })
        }.onFailure {
            Log.e("LoginScreen", "Không thể đăng ký callback Facebook: ${it.message}")
            authViewModel.showAuthMessage("Facebook SDK chưa cấu hình đúng")
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.checkCurrentUser(context)
    }

    LaunchedEffect(uiState.currentUser?.uid, uiState.userDocument?.uid) {
        if (uiState.currentUser != null || (uiState.userDocument != null && uiState.userDocument?.uid?.startsWith("GUEST") == true)) {
            onLoginSuccess()
        }
    }

    val facebookConfigured = remember {
        runCatching {
            val appId = context.getString(R.string.facebook_app_id)
            appId.isNotBlank() && appId != "0"
        }.getOrDefault(false)
    }

    val savedGuestName = remember(context) {
        authViewModel.getSavedGuestNickname(context) ?: ""
    }

    LoginScreen(
        uiState = uiState,
        facebookConfigured = facebookConfigured,
        onEmailChange = authViewModel::onEmailChange,
        onPasswordChange = authViewModel::onPasswordChange,
        onDisplayNameChange = authViewModel::onDisplayNameChange,
        onSubmit = authViewModel::submitAuth,
        onToggleMode = authViewModel::toggleMode,
        onGoogleLogin = {
            val webClientIdRes = context.resources.getIdentifier(
                "default_web_client_id",
                "string",
                context.packageName
            )
            if (webClientIdRes == 0) {
                authViewModel.showAuthMessage(
                    "Thiếu Google Web Client ID. Thêm SHA-1 trong Firebase Console, tải lại google-services.json hoặc đặt GOOGLE_WEB_CLIENT_ID trong local.properties"
                )
                return@LoginScreen
            }
            coroutineScope.launch {
                runCatching {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(context.getString(webClientIdRes))
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(
                        request = request,
                        context = context
                    )
                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        authViewModel.loginWithGoogleIdToken(googleIdTokenCredential.idToken)
                    } else {
                        Log.e("LoginScreen", "Unrecognized credential type: ${credential.type}")
                        authViewModel.showAuthMessage("Loại xác thực không được hỗ trợ")
                    }
                }.onFailure { e ->
                    Log.e("LoginScreen", "Google login error: ${e.message}", e)
                    if (e is GetCredentialException) {
                        authViewModel.showAuthMessage("Đăng nhập Google thất bại (mã lỗi từ Credential Manager)")
                    } else {
                        authViewModel.showAuthMessage("Lỗi khi đăng nhập Google")
                    }
                }
            }
        },
        onFacebookLogin = {
            if (!facebookConfigured) {
                authViewModel.showAuthMessage(
                    "Chưa cấu hình Facebook. Đặt FACEBOOK_APP_ID và FACEBOOK_CLIENT_TOKEN trong local.properties"
                )
                return@LoginScreen
            }
            val activity = context as? androidx.activity.ComponentActivity ?: return@LoginScreen
            if (loginManager == null) {
                authViewModel.showAuthMessage("Facebook SDK chưa sẵn sàng")
                return@LoginScreen
            }
            runCatching {
                loginManager.logIn(activity, callbackManager, listOf("email", "public_profile"))
            }.onFailure {
                Log.e("LoginScreen", "Facebook login lỗi: ${it.message}")
                authViewModel.showAuthMessage("Không thể khởi tạo đăng nhập Facebook")
            }
        },
        onGuestLogin = { nickname -> authViewModel.loginAsGuest(context, nickname) },
        savedGuestNickname = savedGuestName
    )
}

@Composable
private fun LoginScreen(
    uiState: com.vunv.n5nihongo.ui.profile.ProfileUiState,
    facebookConfigured: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onToggleMode: () -> Unit,
    onGoogleLogin: () -> Unit,
    onFacebookLogin: () -> Unit,
    onGuestLogin: (String) -> Unit,
    savedGuestNickname: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_logo),
            contentDescription = "Logo ứng dụng",
            modifier = Modifier.size(100.dp)
        )
        Text(
            text = "Chào mừng bạn đến với hành trình học N5",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = if (uiState.isLoginMode) "Đăng nhập để bắt đầu học" else "Tạo tài khoản mới",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (!uiState.isLoginMode) {
            OutlinedTextField(
                value = uiState.displayNameInput,
                onValueChange = onDisplayNameChange,
                label = { Text("Tên hiển thị") },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = { Text("Mật khẩu") },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        )

        uiState.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(MintPrimary, SkySecondary)),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(enabled = !uiState.isLoading) { onSubmit() }
        ) {
            Text(
                text = if (uiState.isLoading) "Đang xử lý…" else if (uiState.isLoginMode) "Đăng nhập" else "Đăng ký",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        TextButton(onClick = onToggleMode) {
            Text(
                if (uiState.isLoginMode) "Chưa có tài khoản? Đăng ký"
                else "Đã có tài khoản? Đăng nhập"
            )
        }

        var showGuestDialog by remember { mutableStateOf(false) }
        var guestNickname by remember(savedGuestNickname) { mutableStateOf(savedGuestNickname) }

        TextButton(onClick = { showGuestDialog = true }) {
            Text("Đăng nhập bằng Khách (Lưu local) 👤", color = MintPrimary, fontWeight = FontWeight.Bold)
        }

        if (showGuestDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showGuestDialog = false },
                title = { Text("Đăng nhập Khách", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tiến độ học tập và XP của bạn sẽ được lưu trực tiếp trên thiết bị này và không đồng bộ đám mây.", style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = guestNickname,
                            onValueChange = { textValue -> guestNickname = textValue },
                            label = { Text("Tên khách của bạn") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MintPrimary),
                        onClick = {
                            if (guestNickname.isNotBlank()) {
                                onGuestLogin(guestNickname)
                                showGuestDialog = false
                            }
                        }
                    ) {
                        Text("Xác nhận", color = Color.White)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showGuestDialog = false }) {
                        Text("Hủy")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FilledIconButton(
                onClick = onGoogleLogin,
                enabled = !uiState.isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                Text("G", fontWeight = FontWeight.Bold)
            }
            if (facebookConfigured) {
                Spacer(modifier = Modifier.size(10.dp))
                FilledIconButton(
                    onClick = onFacebookLogin,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.size(48.dp)
                ) {
                    Text("f", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
