package com.vunv.n5nihongo.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.data.quiz.GeneratedQuizQuestion
import com.vunv.n5nihongo.ui.theme.LightBackground
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SurfaceWhite
import com.vunv.n5nihongo.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: MockExamViewModel = viewModel(factory = MockExamViewModel.factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isFinished) "Kết quả Thi thử N5" else "Đề Thi Thử JLPT N5",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (!uiState.isLoading && !uiState.isFinished) {
                        IconButton(onClick = { showSheet = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Danh sách câu hỏi")
                        }
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
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MintPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Đang tạo đề thi thử N5 ngẫu nhiên...", color = Color.Gray, fontSize = 14.sp)
                    }
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Lỗi tải đề thi",
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
                        ) {
                            Text("Quay lại", color = Color.White)
                        }
                    }
                }
                uiState.isFinished -> {
                    MockExamResultScreen(
                        uiState = uiState,
                        onBack = onBack,
                        onNavigate = onNavigate
                    )
                }
                else -> {
                    val currentQuestion = uiState.questions.getOrNull(uiState.currentIndex)
                    if (currentQuestion != null) {
                        ActiveExamContent(
                            question = currentQuestion,
                            uiState = uiState,
                            onOptionSelected = { option ->
                                viewModel.selectOption(uiState.currentIndex, option)
                            },
                            onPrevClick = { viewModel.setCurrentIndex(uiState.currentIndex - 1) },
                            onNextClick = { viewModel.setCurrentIndex(uiState.currentIndex + 1) },
                            onSubmitClick = { viewModel.submitExam() }
                        )
                    }
                }
            }

            // Bottom sheet chứa bảng câu hỏi điều hướng nhanh
            if (showSheet && !uiState.isFinished) {
                ModalBottomSheet(
                    onDismissRequest = { showSheet = false },
                    containerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Bảng câu hỏi (30 câu)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Button(
                                onClick = {
                                    showSheet = false
                                    viewModel.submitExam()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043))
                            ) {
                                Text("Nộp bài thi", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            items(uiState.questions.size) { index ->
                                val isAnswered = uiState.selectedAnswers.containsKey(index)
                                val isCurrent = uiState.currentIndex == index

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isCurrent -> MintPrimary
                                                isAnswered -> MintPrimary.copy(alpha = 0.15f)
                                                else -> Color.LightGray.copy(alpha = 0.4f)
                                            }
                                        )
                                        .border(
                                            1.5.dp,
                                            if (isCurrent) MintPrimary else Color.Transparent,
                                            CircleShape
                                        )
                                        .clickable {
                                            viewModel.setCurrentIndex(index)
                                            showSheet = false
                                        }
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = when {
                                            isCurrent -> Color.White
                                            isAnswered -> MintPrimary
                                            else -> Color.DarkGray
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveExamContent(
    question: GeneratedQuizQuestion,
    uiState: MockExamUiState,
    onOptionSelected: (String) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    val currentNum = uiState.currentIndex + 1
    val total = uiState.totalQuestions
    val chosenOption = uiState.selectedAnswers[uiState.currentIndex]

    // Format thời gian: mm:ss
    val minutes = uiState.timeRemaining / 60
    val seconds = uiState.timeRemaining % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Đồng hồ đếm ngược & Chỉ số câu hỏi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = timeString,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "Câu $currentNum/$total",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
        }

        LinearProgressIndicator(
            progress = { currentNum.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MintPrimary,
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )

        // Thẻ phân loại kỹ năng của câu hỏi
        Badge(
            containerColor = MintPrimary.copy(alpha = 0.12f),
            contentColor = MintPrimary
        ) {
            val categoryVietnamese = when (question.category) {
                com.vunv.n5nihongo.data.quiz.QuizCategory.VOCABULARY -> "Từ vựng (Goi)"
                com.vunv.n5nihongo.data.quiz.QuizCategory.GRAMMAR -> "Ngữ pháp (Bunpou)"
                com.vunv.n5nihongo.data.quiz.QuizCategory.KANJI -> "Chữ Hán (Kanji)"
            }
            Text(categoryVietnamese.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold)
        }

        // Thẻ hiển thị câu hỏi Glassmorphism
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Các tùy chọn đáp án
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            question.options.forEach { option ->
                val isSelected = chosenOption == option
                val buttonColor = if (isSelected) MintPrimary.copy(alpha = 0.08f) else Color.White
                val borderColor = if (isSelected) MintPrimary else Color.LightGray.copy(alpha = 0.4f)
                val textColor = if (isSelected) MintPrimary else Color.Black

                Button(
                    onClick = { onOptionSelected(option) },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, borderColor, RoundedCornerShape(14.dp)),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = MintPrimary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = option,
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Thanh điều hướng câu hỏi dưới cùng
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevClick,
                enabled = uiState.currentIndex > 0
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Câu trước",
                    tint = if (uiState.currentIndex > 0) MintPrimary else Color.Gray.copy(alpha = 0.4f)
                )
            }

            if (currentNum == total) {
                Button(
                    onClick = onSubmitClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7043)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nộp bài thi", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = onNextClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Câu tiếp theo", color = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MockExamResultScreen(
    uiState: MockExamUiState,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val isPassed = uiState.correctCount >= 18 // Đạt trên 60% (18/30 câu)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vòng tròn điểm số phát sáng
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(150.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isPassed) MintPrimary else Color(0xFFFF7043)).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${uiState.correctCount}/${uiState.totalQuestions}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPassed) MintPrimary else Color(0xFFFF7043)
                )
                Text(
                    text = "Đúng",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        // Tag báo Đạt/Trượt thi thử
        Badge(
            containerColor = if (isPassed) MintPrimary.copy(alpha = 0.15f) else Color(0xFFFFEBEE),
            contentColor = if (isPassed) MintPrimary else Color(0xFFC62828)
        ) {
            Text(
                text = if (isPassed) "Chúc mừng! BẠN ĐÃ ĐẠT 🎉" else "Cần cố gắng thêm: CHƯA ĐẠT 🌸",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // Bảng Skill Breakdown
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Đánh giá điểm thành phần 📊",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )

                SkillBar(
                    label = "Từ vựng (Goi)",
                    correct = uiState.skillsBreakdown.correctVocab,
                    total = uiState.skillsBreakdown.totalVocab,
                    color = MintPrimary
                )

                SkillBar(
                    label = "Ngữ pháp (Bunpou)",
                    correct = uiState.skillsBreakdown.correctGrammar,
                    total = uiState.skillsBreakdown.totalGrammar,
                    color = Color(0xFFFF9800)
                )

                SkillBar(
                    label = "Chữ Hán (Kanji)",
                    correct = uiState.skillsBreakdown.correctKanji,
                    total = uiState.skillsBreakdown.totalKanji,
                    color = Color(0xFF2196F3)
                )
            }
        }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        "AI Sensei Báo cáo & Lộ trình 🌸",
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
                    val rawAnalysis = uiState.aiAnalysis.orEmpty()
                    // Render văn bản nhận xét
                    Text(
                        text = cleanTextFromButtons(rawAnalysis),
                        fontSize = 13.5.sp,
                        color = Color.DarkGray,
                        lineHeight = 19.sp
                    )

                    // Phân tích và render các nút hành động điều hướng từ AI
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
            Text("Quay lại phòng luyện thi", color = Color.DarkGray, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SkillBar(
    label: String,
    correct: Int,
    total: Int,
    color: Color
) {
    val percent = if (total > 0) correct.toFloat() / total.toFloat() else 0f
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, color = Color.DarkGray)
            Text("$correct/$total câu (${(percent * 100).toInt()}%)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color.LightGray.copy(alpha = 0.25f)
        )
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
