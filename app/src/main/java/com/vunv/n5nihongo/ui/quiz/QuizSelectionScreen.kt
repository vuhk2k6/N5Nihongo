package com.vunv.n5nihongo.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunv.n5nihongo.ui.components.GlassCard
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SurfaceWhite
import com.vunv.n5nihongo.ui.theme.TextPrimary
import com.vunv.n5nihongo.ui.theme.LightBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizSelectionScreen(
    onQuizSelected: (Int) -> Unit,
    onStartMockExam: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trung tâm Luyện thi N5",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = LightBackground
    ) { innerPadding ->
        val lessons = (1..25).toList()

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Banner Đề thi thử JLPT N5 chuẩn hóa dạng Premium Glassmorphic Card
            item(span = { GridItemSpan(3) }) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MintPrimary,
                                        MintPrimary.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "JLPT N5 MOCK EXAM",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                Badge(
                                    containerColor = Color.White.copy(alpha = 0.25f),
                                    contentColor = Color.White
                                ) {
                                    Text("Premium AI", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "Đề Thi Thử JLPT N5 Chuẩn Hóa 🏆",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Text(
                                text = "Đề thi gồm 30 câu hỏi ngẫu nhiên tổng hợp từ vựng, ngữ pháp và chữ Hán N5, giới hạn thời gian làm bài 45 phút kèm phân tích chi tiết từ AI Sensei.",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("45 phút", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("•", color = Color.White.copy(alpha = 0.6f))
                                Text("30 Câu hỏi", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onStartMockExam,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Text(
                                    text = "Bắt đầu làm bài thi",
                                    color = MintPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Tiêu đề danh sách luyện tập theo bài
            item(span = { GridItemSpan(3) }) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Luyện tập theo từng bài học 📚",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // 3. Lưới bài học 1-25
            items(lessons) { lessonId ->
                GlassCard(
                    modifier = Modifier
                        .aspectRatio(1.1f)
                        .fillMaxWidth()
                        .clickable { onQuizSelected(lessonId) }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Bài $lessonId",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lessonId in 1..2) "Chữ cái" else if (lessonId == 3) "Số đếm" else "Minna",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(3) }) {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
