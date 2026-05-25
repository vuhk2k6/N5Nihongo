package com.vunv.n5nihongo.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunv.n5nihongo.ui.theme.LightBackground
import com.vunv.n5nihongo.ui.theme.MintPrimary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbersTimeRoute(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Số đếm", "Đồng hồ", "Lịch")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Numbers & Time Foundation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onNavigate("aiQuiz?prompt=Trắc nghiệm số đếm, giờ giấc, ngày tháng tiếng Nhật N5")
                },
                icon = { Icon(Icons.Default.Quiz, contentDescription = null) },
                text = { Text("Luyện trắc nghiệm AI") },
                containerColor = MintPrimary,
                contentColor = Color.White
            )
        },
        containerColor = LightBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> NumbersTab()
                1 -> TimeTab()
                2 -> CalendarTab()
            }
        }
    }
}

@Composable
fun NumbersTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bảng quy tắc số đếm cơ bản:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        // Static table for demo
        val basics = listOf(
            1 to "いち", 2 to "に", 3 to "さん", 4 to "よん/し", 5 to "ご",
            6 to "ろく", 7 to "なな/しち", 8 to "はち", 9 to "きゅう/く", 10 to "じゅう"
        )
        basics.forEach { (num, read) ->
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(num.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(read, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Biến âm cần lưu ý:", color = Color.Red, fontWeight = FontWeight.Bold)
        Text("300: さんびゃく", color = Color.Red)
        Text("600: ろっぴゃく", color = Color.Red)
        Text("800: はっぴゃく", color = Color.Red)
        Text("3000: さんぜん", color = Color.Red)
        Text("8000: はっせん", color = Color.Red)
    }
}

@Composable
fun TimeTab() {
    var hours by remember { mutableIntStateOf(1) }
    var minutes by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        InteractiveClock(
            hours = hours,
            minutes = minutes,
            onTimeChanged = { h, m ->
                hours = h
                minutes = m
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${hours}時 ${if (minutes > 0) "${minutes}分" else ""}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Demo mapping
                val hRead = when (hours) {
                    4 -> "よじ"
                    7 -> "しちじ"
                    9 -> "くじ"
                    else -> "$hours じ" // Simplified
                }
                
                Text(
                    text = hRead,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun InteractiveClock(
    hours: Int,
    minutes: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    val radius = 120.dp
    
    Box(
        modifier = Modifier
            .size(radius * 2)
            .background(Color.White, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val touchOffset = change.position - center
                    val angle = (atan2(touchOffset.y.toDouble(), touchOffset.x.toDouble()) * 180 / Math.PI + 90) % 360
                    val normalizedAngle = if (angle < 0) angle + 360 else angle
                    
                    // Map angle to hours (1-12)
                    var newHour = (normalizedAngle / 30).roundToInt()
                    if (newHour == 0) newHour = 12
                    
                    onTimeChanged(newHour, 0)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val r = size.width / 2
            
            // Draw face
            drawCircle(color = Color.LightGray, radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
            
            // Draw Hour Hand
            val hourAngle = (hours * 30 - 90) * (Math.PI / 180)
            val hLength = r * 0.5f
            val hEnd = Offset(
                x = center.x + (cos(hourAngle) * hLength).toFloat(),
                y = center.y + (sin(hourAngle) * hLength).toFloat()
            )
            drawLine(color = Color.Black, start = center, end = hEnd, strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
            
            // Draw Minute Hand
            val minAngle = (minutes * 6 - 90) * (Math.PI / 180)
            val mLength = r * 0.8f
            val mEnd = Offset(
                x = center.x + (cos(minAngle) * mLength).toFloat(),
                y = center.y + (sin(minAngle) * mLength).toFloat()
            )
            drawLine(color = Color.Red, start = center, end = mEnd, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            
            // Draw Center dot
            drawCircle(color = Color.Black, radius = 6.dp.toPx())
        }
    }
}

@Composable
fun CalendarTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ngày trong tháng đặc biệt:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        val days = listOf(
            1 to "ついたち", 2 to "ふつか", 3 to "みっか", 4 to "よっか", 5 to "いつか",
            6 to "むいか", 7 to "なのか", 8 to "ようか", 9 to "ここのか", 10 to "とおか",
            14 to "じゅうよっか", 20 to "はつか", 24 to "にじゅうよっか"
        )
        days.forEach { (d, read) ->
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ngày $d", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                Text(read, fontSize = 18.sp)
            }
        }
    }
}
