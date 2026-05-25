package com.vunv.n5nihongo.ui.quiz

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.ui.components.JapaneseAudioButton
import com.vunv.n5nihongo.ui.components.rememberJapaneseSpeaker
import com.vunv.n5nihongo.data.quiz.QuizCategory
import com.vunv.n5nihongo.data.quiz.QuizPromptType
import androidx.compose.foundation.background
import com.vunv.n5nihongo.ui.theme.LightBackground
import com.vunv.n5nihongo.ui.theme.MintPrimary

@Composable
fun QuizRoute(
    lessonId: Int,
    selectedWordIds: List<Int> = emptyList(),
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val quizViewModel: QuizViewModel = viewModel(
        factory = QuizViewModel.factory(
            lessonId = lessonId,
            selectedWordIds = selectedWordIds
        )
    )
    val uiState by quizViewModel.uiState.collectAsStateWithLifecycle()
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    val speak = rememberJapaneseSpeaker()
    var isResultDialogDismissed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    LaunchedEffect(uiState.isAnswerCorrect) {
        when (uiState.isAnswerCorrect) {
            true -> toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            false -> toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 180)
            null -> Unit
        }
    }
    LaunchedEffect(uiState.isFinished) {
        if (!uiState.isFinished) {
            isResultDialogDismissed = false
        }
    }

    QuizScreen(
        uiState = uiState,
        onSelectAnswer = quizViewModel::submitAnswer,
        onNext = quizViewModel::moveToNextQuestion,
        onPlayAudio = speak,
        onResultDismiss = { 
            isResultDialogDismissed = true
            onBack()
        },
        showResultDialog = uiState.isFinished && !isResultDialogDismissed,
        modifier = modifier
    )
}

@Composable
private fun QuizScreen(
    uiState: QuizUiState,
    onSelectAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onResultDismiss: () -> Unit,
    showResultDialog: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (uiState.totalQuestions == 0) 0f
    else (uiState.questionIndex + 1).toFloat() / uiState.totalQuestions.toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Câu ${uiState.questionIndex + 1}/${uiState.totalQuestions}",
            style = MaterialTheme.typography.titleMedium
        )

        if (uiState.isLoading) {
            Text("Đang tạo đề...")
            return
        }

        val question = uiState.currentQuestion
        if (question == null) {
            Text("Không có dữ liệu câu hỏi.")
            return
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categoryLabel = when (question.category) {
                    QuizCategory.VOCABULARY -> "Từ vựng"
                    QuizCategory.GRAMMAR -> "Ngữ pháp"
                    QuizCategory.KANJI -> "Kanji"
                }
                val instruction = when (question.promptType) {
                    QuizPromptType.WORD_TO_MEANING -> "Chọn nghĩa đúng:"
                    QuizPromptType.MEANING_TO_WORD -> "Chọn từ vựng đúng:"
                    QuizPromptType.LISTENING -> "Nghe và chọn nghĩa đúng:"
                    QuizPromptType.GRAMMAR_MEANING_TO_PATTERN ->
                        "Chọn mẫu ngữ pháp tiếng Nhật có nghĩa sau:"
                    QuizPromptType.GRAMMAR_PATTERN_TO_MEANING ->
                        "Chọn nghĩa tiếng Việt của mẫu ngữ pháp:"
                    QuizPromptType.GRAMMAR_EXAMPLE -> "Chọn nghĩa câu ví dụ đúng:"
                    QuizPromptType.KANJI_MEANING -> "Chọn nghĩa Kanji đúng:"
                    QuizPromptType.KANJI_READING -> "Chọn cách đọc đúng:"
                }

                val shouldAutoPlay = question.promptType == QuizPromptType.LISTENING ||
                    question.promptType == QuizPromptType.GRAMMAR_EXAMPLE ||
                    (question.audioText.isNotBlank() && question.category == QuizCategory.KANJI)

                LaunchedEffect(question) {
                    if (shouldAutoPlay && question.audioText.isNotBlank()) {
                        onPlayAudio(question.audioText)
                    }
                }

                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (question.promptType) {
                        QuizPromptType.LISTENING -> {
                            JapaneseAudioButton(
                                text = question.audioText,
                                onSpeak = onPlayAudio,
                                size = 48.dp
                            )
                        }
                        QuizPromptType.GRAMMAR_MEANING_TO_PATTERN,
                        QuizPromptType.GRAMMAR_PATTERN_TO_MEANING -> {
                            Text(
                                text = question.prompt,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MintPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            Text(
                                question.prompt,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MintPrimary
                            )
                            if (question.audioText.isNotBlank() &&
                                question.promptType != QuizPromptType.MEANING_TO_WORD &&
                                question.promptType != QuizPromptType.GRAMMAR_MEANING_TO_PATTERN &&
                                question.promptType != QuizPromptType.GRAMMAR_PATTERN_TO_MEANING
                            ) {
                                JapaneseAudioButton(
                                    text = question.audioText,
                                    onSpeak = onPlayAudio
                                )
                            }
                        }
                    }
                }
                Text(instruction, style = MaterialTheme.typography.titleMedium)
            }
        }

        question.options.forEach { option ->
            val selected = uiState.selectedAnswer == option
            OutlinedButton(
                onClick = { onSelectAnswer(option) },
                enabled = uiState.selectedAnswer == null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = option + if (selected) "  ✓" else "",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (uiState.selectedAnswer != null) {
            Text(
                text = if (uiState.isAnswerCorrect == true) "Chính xác!" else "Sai rồi!",
                style = MaterialTheme.typography.titleMedium
            )
            Button(onClick = onNext) {
                Text("Câu tiếp theo")
            }
        }
    }

    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = onResultDismiss,
            title = { Text("Kết quả bài Quiz") },
            text = {
                val percent = if (uiState.totalQuestions == 0) 0
                else (uiState.correctAnswers * 100) / uiState.totalQuestions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Bạn trả lời đúng ${uiState.correctAnswers}/${uiState.totalQuestions} câu ($percent%)."
                    )
                    Text(
                        if (uiState.lessonPassed) {
                            "Đạt ≥${uiState.passPercent}% — bài học được tính là hoàn thành."
                        } else {
                            "Cần đạt ≥${uiState.passPercent}% để hoàn thành bài. Hãy ôn lại và thử lại."
                        },
                        color = if (uiState.lessonPassed) MintPrimary else MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(onClick = onResultDismiss) { Text("Đã hiểu") }
            }
        )
    }
}
