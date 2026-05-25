package com.vunv.n5nihongo.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.ui.theme.LightBackground
import com.vunv.n5nihongo.ui.theme.MintPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuizScreen(
    prompt: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: AiQuizViewModel = viewModel(factory = AiQuizViewModel.factory(prompt))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MintPrimary)
                        Text("Bài kiểm tra AI Sensei", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = LightBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))
                        
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                              ) {
                                CircularProgressIndicator(
                                    color = MintPrimary,
                                    strokeWidth = 3.5.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "AI Sensei đang biên soạn...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Bài trắc nghiệm của bạn được chuẩn bị offline từ cơ sở dữ liệu local để đảm bảo tốc độ tối đa và tránh nghẽn mạng. 🌸",
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = Color(0xFFE57373),
                                    modifier = Modifier.size(72.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Text(
                                    text = "Thông báo hệ thống",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = uiState.errorMessage ?: "Lỗi không xác định",
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = Color.DarkGray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text(
                                "Quay lại",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                uiState.isFinished -> {
                    FinishedScreen(
                        uiState = uiState,
                        onBack = onBack,
                        onNavigate = onNavigate
                    )
                }
                else -> {
                    val currentQuestion = uiState.questions.getOrNull(uiState.currentIndex)
                    if (currentQuestion != null) {
                        QuestionContent(
                            question = currentQuestion,
                            currentIndex = uiState.currentIndex,
                            totalQuestions = uiState.questions.size,
                            selectedOption = uiState.selectedOption,
                            isAnswered = uiState.isAnswered,
                            isCorrect = uiState.isCorrect,
                            onSubmit = viewModel::submitAnswer,
                            onNext = viewModel::nextQuestion
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionContent(
    question: AiQuestion,
    currentIndex: Int,
    totalQuestions: Int,
    selectedOption: String?,
    isAnswered: Boolean,
    isCorrect: Boolean,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit
) {
    val progress = (currentIndex + 1).toFloat() / totalQuestions.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tiến độ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Câu hỏi ${currentIndex + 1}/$totalQuestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Đúng: ${if (isAnswered && isCorrect) "✓" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MintPrimary
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = MintPrimary,
            trackColor = Color.LightGray.copy(alpha = 0.4f)
        )

        // Thẻ câu hỏi Glassmorphism
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Danh sách lựa chọn
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            question.options.forEach { option ->
                val isSelected = selectedOption == option
                val isThisCorrect = option == question.correctAnswer
                
                val buttonColor = when {
                    !isAnswered -> Color.White
                    isSelected && isCorrect -> Color(0xFFE8F5E9)
                    isSelected && !isCorrect -> Color(0xFFFFEBEE)
                    isThisCorrect -> Color(0xFFE8F5E9)
                    else -> Color.White
                }

                val borderColor = when {
                    !isAnswered -> if (isSelected) MintPrimary else Color.LightGray.copy(alpha = 0.5f)
                    isSelected && isCorrect -> Color(0xFF4CAF50)
                    isSelected && !isCorrect -> Color(0xFFF44336)
                    isThisCorrect -> Color(0xFF4CAF50)
                    else -> Color.LightGray.copy(alpha = 0.5f)
                }

                val textColor = when {
                    !isAnswered -> Color.Black
                    isSelected && isCorrect -> Color(0xFF2E7D32)
                    isSelected && !isCorrect -> Color(0xFFC62828)
                    isThisCorrect -> Color(0xFF2E7D32)
                    else -> Color.Gray
                }

                Button(
                    onClick = { if (!isAnswered) onSubmit(option) },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAnswered) {
                            when {
                                isThisCorrect -> Icon(Icons.Default.CheckCircle, contentDescription = "Đúng", tint = Color(0xFF4CAF50))
                                isSelected && !isCorrect -> Icon(Icons.Default.Cancel, contentDescription = "Sai", tint = Color(0xFFF44336))
                            }
                        }
                    }
                }
            }
        }

        // Lời giải thích khi đã trả lời
        AnimatedVisibility(
            visible = isAnswered,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, MintPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "💡 Giải thích bài học:",
                            fontWeight = FontWeight.Bold,
                            color = MintPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = question.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray
                        )
                    }
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Câu tiếp theo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FinishedScreen(
    uiState: AiQuizUiState,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val correctCount = uiState.correctCount
    val totalQuestions = uiState.questions.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vòng tròn điểm số phát sáng
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(MintPrimary.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$correctCount/$totalQuestions",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary
                )
                Text(
                    text = "Đúng",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        Text(
            text = "Kết quả bài luyện tập 🌸",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        // Hộp nhận xét từ AI Sensei (Coaching Report)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.6.dp, MintPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "AI Sensei Đánh giá chi tiết 📝",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MintPrimary
                    )
                }

                if (uiState.isAiThinking && uiState.aiAnalysis.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = MintPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI Sensei đang biên soạn lời khuyên...", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    val rawAnalysis = uiState.aiAnalysis ?: "Cảm ơn bạn đã hoàn thành bài thi trắc nghiệm! AI Sensei đang kết xuất nội dung đánh giá."
                    // Render văn bản nhận xét sạch
                    Text(
                        text = cleanTextFromButtons(rawAnalysis),
                        fontSize = 13.5.sp,
                        color = Color.DarkGray,
                        lineHeight = 19.sp
                    )

                    // Phân tích và vẽ các nút hành động từ AI
                    val actionButtons = parseAiSuggestions(rawAnalysis)
                    if (actionButtons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        actionButtons.forEach { btn ->
                            Button(
                                onClick = { onNavigate(btn.route) },
                                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(btn.title, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Quay lại", color = Color.DarkGray, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Lớp hỗ trợ nút điều hướng
private data class AiButton(val title: String, val route: String)

private fun parseAiSuggestions(text: String): List<AiButton> {
    val regex = Regex("\\[([^\\]]+)\\]\\(navigate:([^\\)]+)\\)")
    val buttons = mutableListOf<AiButton>()
    val matches = regex.findAll(text)
    for (match in matches) {
        val title = match.groups[1]?.value.orEmpty().trim()
        val route = match.groups[2]?.value.orEmpty().trim()
        if (title.isNotEmpty() && route.isNotEmpty()) {
            buttons.add(AiButton(title, route))
        }
    }
    return buttons
}

private fun cleanTextFromButtons(text: String): String {
    val regex = Regex("\\[([^\\]]+)\\]\\(navigate:([^\\)]+)\\)")
    return regex.replace(text, "").trim()
}
