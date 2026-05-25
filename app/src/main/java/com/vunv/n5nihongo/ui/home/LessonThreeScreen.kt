package com.vunv.n5nihongo.ui.home

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import com.vunv.n5nihongo.N5NihongoApp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private data class CountingItem(
    val number: Int,
    val kanji: String,
    val reading: String,
    val isHotSpot: Boolean
)

private data class StoreScenario(
    val icon: String,
    val label: String,
    val quantity: Int,
    val counter: String
)

@Composable
fun LessonThreeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tts = rememberJapaneseTts(context)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { CountingSection(onSpeak = { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, null) }) }
        item { DateSection(onSpeak = { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, null) }) }
        item { TimeSection(onSpeak = { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, null) }) }
        item { ReminderSection(context = context) }
    }
}

@Composable
private fun CountingSection(onSpeak: (String) -> Unit) {
    val items = remember {
        listOf(
            CountingItem(1, "一", "いち", true),
            CountingItem(2, "二", "に", false),
            CountingItem(3, "三", "さん", false),
            CountingItem(4, "四", "よん", false),
            CountingItem(5, "五", "ご", false),
            CountingItem(6, "六", "ろく", true),
            CountingItem(7, "七", "なな", false),
            CountingItem(8, "八", "はち", true),
            CountingItem(9, "九", "きゅう", false),
            CountingItem(10, "十", "じゅう", true)
        )
    }
    var selected by remember { mutableStateOf<CountingItem?>(null) }
    var storeScenario by remember { mutableStateOf(randomStoreScenario()) }
    var storeResult by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Bài 3.1 - Số đếm", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Điểm nóng biến âm: 1, 6, 8, 10", style = MaterialTheme.typography.bodyMedium)

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(170.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items) { item ->
                    val isSelected = selected?.number == item.number
                    Box(
                        modifier = Modifier
                            .background(
                                if (item.isHotSpot) Color(0xFFFFCCBC) else Color(0xFFE8F5E9),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selected = item
                                onSpeak(item.reading)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSelected) "${item.number}\n${item.kanji}" else item.number.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Text(
                selected?.let { "Đã chọn: ${it.number} - ${it.kanji} (${it.reading})" }
                    ?: "Chạm vào số để xem Kanji và nghe phát âm."
            )

            Text("Mini-game: Cửa hàng tiện lợi", style = MaterialTheme.typography.titleMedium)
            Text("${storeScenario.icon} ${storeScenario.quantity} ${storeScenario.label}")
            val correct = japaneseCounter(storeScenario.quantity, storeScenario.counter)
            val options = remember(storeScenario) { (listOf(correct) + buildCounterDistractors(storeScenario)).shuffled().take(4) }
            options.forEach { option ->
                Button(
                    onClick = {
                        storeResult = if (option == correct) "Đúng rồi!" else "Sai, đáp án đúng: $correct"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(option) }
            }
            Text(storeResult)
            Button(onClick = { storeScenario = randomStoreScenario(); storeResult = "" }) { Text("Câu mới") }
        }
    }
}

@Composable
private fun DateSection(onSpeak: (String) -> Unit) {
    val currentMonth = remember { YearMonth.now() }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var gamePrompt by remember { mutableStateOf("Hãy tìm ngày Quốc khánh Nhật Bản (11/2)") }
    var gameAnswer by remember { mutableStateOf(LocalDate.of(LocalDate.now().year, 2, 11)) }
    var gameResult by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bài 3.2 - Thử thách Ngày tháng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${currentMonth.monthValue}/${currentMonth.year}")

            val days = (1..currentMonth.lengthOfMonth()).toList()
            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(180.dp)) {
                items(days) { day ->
                    val isSpecial = day in listOf(1,2,3,4,5,6,7,8,9,10,14,20,24)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .background(if (isSpecial) Color(0xFFFFEBEE) else Color(0xFFF1F8E9), RoundedCornerShape(8.dp))
                            .clickable {
                                val date = LocalDate.of(currentMonth.year, currentMonth.monthValue, day)
                                selectedDate = date
                                onSpeak(readDate(date.dayOfMonth))
                                gameResult = if (date == gameAnswer) "Chính xác!" else "Chưa đúng."
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(day.toString()) }
                }
            }

            Text(selectedDate?.let { "Ngày ${it.dayOfMonth}: ${readDate(it.dayOfMonth)}" } ?: "Chạm vào ngày để nghe cách đọc.")
            Text("Mini-game: $gamePrompt")
            Text(gameResult)
            Button(onClick = {
                val day = listOf(20, 14, 24, 7).random()
                gameAnswer = LocalDate.of(currentMonth.year, currentMonth.monthValue, day)
                gamePrompt = "Ngày $day tháng này là ngày mấy?"
                gameResult = ""
            }) { Text("Đổi thử thách") }
        }
    }
}

@Composable
private fun TimeSection(onSpeak: (String) -> Unit) {
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current
    var challengeHour by remember { mutableIntStateOf(7) }
    var challengeMinute by remember { mutableIntStateOf(30) }
    var challengeResult by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bài 3.3 - Giờ giấc (đồng hồ kim)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            AnalogClock(hour = hour, minute = minute)
            Text("Cách đọc: ${readTime(hour, minute)}")

            Text("Giờ: $hour")
            Slider(value = hour.toFloat(), onValueChange = { hour = it.toInt() }, valueRange = 0f..23f)
            Text("Phút: $minute")
            Slider(value = minute.toFloat(), onValueChange = {
                val old = minute
                minute = it.toInt()
                if (old != minute) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }, valueRange = 0f..59f)

            Text("Luyện nghe: Ima nan-ji desu ka? -> $challengeHour:$challengeMinute")
            Button(onClick = {
                onSpeak("いま　なんじ　ですか")
                challengeResult = if (hour == challengeHour && minute == challengeMinute) "Đúng giờ rồi!" else "Sai, hãy xoay đúng kim."
            }) { Text("Kiểm tra") }
            Text(challengeResult)
            Button(onClick = {
                challengeHour = (0..23).random()
                challengeMinute = listOf(0, 10, 15, 20, 30, 45, 50).random()
                challengeResult = ""
            }) { Text("Mốc giờ mới") }
        }
    }
}

@Composable
private fun ReminderSection(context: Context) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bài 3.4 - Nhắc nhở thông minh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Thông báo sẽ nhắc theo giờ với câu tiếng Nhật và ngày đặc biệt.")
            Button(onClick = { N5NihongoApp.scheduleSmartTimeReminder(context) }) {
                Text("Kích hoạt lại nhắc nhở WorkManager")
            }
            Button(onClick = {
                WorkManager.getInstance(context).cancelUniqueWork(N5NihongoApp.TIME_REMINDER_WORK_NAME)
            }) { Text("Tắt nhắc nhở") }
        }
    }
}

@Composable
private fun AnalogClock(hour: Int, minute: Int) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            drawCircle(color = Color(0xFFE0F2F1), style = Stroke(width = 6f))
            val center = center
            val radius = size.minDimension / 2f - 16f

            val minuteAngle = Math.toRadians((minute * 6 - 90).toDouble())
            val hourAngle = Math.toRadians((((hour % 12) * 30) + minute * 0.5 - 90).toDouble())

            val minuteEnd = Offset(
                x = center.x + (cos(minuteAngle) * radius * 0.86f).toFloat(),
                y = center.y + (sin(minuteAngle) * radius * 0.86f).toFloat()
            )
            val hourEnd = Offset(
                x = center.x + (cos(hourAngle) * radius * 0.58f).toFloat(),
                y = center.y + (sin(hourAngle) * radius * 0.58f).toFloat()
            )
            drawLine(Color(0xFF26A69A), center, minuteEnd, strokeWidth = 6f)
            drawLine(Color(0xFFE53935), center, hourEnd, strokeWidth = 9f)
            drawCircle(color = Color(0xFF004D40), radius = 8f, center = center)
        }
    }
}

@Composable
private fun rememberJapaneseTts(context: Context): TextToSpeech? {
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }
    return tts
}

private fun randomStoreScenario(): StoreScenario {
    val list = listOf(
        StoreScenario("📚", "quyển sách", 3, "冊"),
        StoreScenario("🧴", "chai nước", 2, "本"),
        StoreScenario("🐶", "con chó", 1, "匹"),
        StoreScenario("📦", "hộp hàng", 8, "個")
    )
    return list.random()
}

private fun japaneseCounter(number: Int, counter: String): String {
    val base = when (number) {
        1 -> "いっ"
        6 -> "ろっ"
        8 -> "はっ"
        10 -> "じゅっ"
        else -> readNumber(number)
    }
    return "$base$counter"
}

private fun buildCounterDistractors(scenario: StoreScenario): List<String> {
    val wrong1 = "${readNumber(scenario.quantity)}本"
    val wrong2 = "${readNumber((scenario.quantity % 10) + 1)}${scenario.counter}"
    val wrong3 = "${readNumber(scenario.quantity)}個"
    return listOf(wrong1, wrong2, wrong3)
}

private fun readNumber(number: Int): String = when (number) {
    1 -> "いち"
    2 -> "に"
    3 -> "さん"
    4 -> "よん"
    5 -> "ご"
    6 -> "ろく"
    7 -> "なな"
    8 -> "はち"
    9 -> "きゅう"
    10 -> "じゅう"
    else -> number.toString()
}

private fun readDate(day: Int): String = when (day) {
    1 -> "ついたち"
    2 -> "ふつか"
    3 -> "みっか"
    4 -> "よっか"
    5 -> "いつか"
    6 -> "むいか"
    7 -> "なのか"
    8 -> "ようか"
    9 -> "ここのか"
    10 -> "とおか"
    14 -> "じゅうよっか"
    20 -> "はつか"
    24 -> "にじゅうよっか"
    else -> "${readNumber(day / 10)}${readNumber(day % 10)}にち"
}

private fun readTime(hour: Int, minute: Int): String {
    val hourText = "${readNumber((hour % 12).let { if (it == 0) 12 else it })}じ"
    val minuteText = when (minute) {
        0 -> "ちょうど"
        1 -> "いっぷん"
        3 -> "さんぷん"
        4 -> "よんぷん"
        6 -> "ろっぷん"
        8 -> "はっぷん"
        10 -> "じゅっぷん"
        else -> "${readNumber(minute)}ふん"
    }
    return "$hourText $minuteText"
}

