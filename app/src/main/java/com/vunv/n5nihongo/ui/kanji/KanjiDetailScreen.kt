package com.vunv.n5nihongo.ui.kanji

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.data.model.Kanji
import com.vunv.n5nihongo.data.model.Word
import com.vunv.n5nihongo.ui.alphabet.DrawPracticeCanvas
import com.vunv.n5nihongo.ui.components.JapaneseAudioButton
import com.vunv.n5nihongo.ui.components.StrokeOrderAnimatedSection
import com.vunv.n5nihongo.ui.components.LessonScreenLoading
import com.vunv.n5nihongo.ui.components.rememberDeferredJapaneseSpeaker
import com.vunv.n5nihongo.ui.theme.MintLight
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SurfaceWhite
import com.vunv.n5nihongo.ui.theme.TextPrimary
import com.vunv.n5nihongo.ui.theme.TextSecondary

@Composable
fun KanjiDetailRoute(
    kanjiId: Int,
    onBack: () -> Unit
) {
    val viewModel: KanjiDetailViewModel =
        viewModel(factory = KanjiDetailViewModel.factory(kanjiId))
    val uiState by viewModel.uiState.collectAsState()
    KanjiDetailScreen(uiState = uiState, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KanjiDetailScreen(
    uiState: KanjiDetailUiState,
    onBack: () -> Unit
) {
    val speak = rememberDeferredJapaneseSpeaker()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.kanji?.let { "${it.character} — ${it.meaning}" } ?: "Chi tiết Kanji",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LessonScreenLoading(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            uiState.kanji == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không tìm thấy chữ Hán này", color = TextSecondary)
                }
            }
            else -> {
                val kanji = uiState.kanji!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MintLight.copy(alpha = 0.18f)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { StrokeOrderAnimationCard(character = kanji.character) }
                    item { StrokePracticeCard(character = kanji.character) }
                    item { KanjiInfoCard(kanji = kanji) }
                    item { ExamplesHeader(count = uiState.examples.size) }
                    items(uiState.examples, key = { it.id }) { word ->
                        ExampleWordCard(word = word, onSpeak = speak)
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StrokeOrderAnimationCard(character: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Thứ tự và chiều viết nét",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            StrokeOrderAnimatedSection(
                character = character,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                height = 240.dp
            )
            Text(
                text = "Nền mờ có số thứ tự; nét sáng màu chạy lần lượt theo đúng hướng viết (lặp lại liên tục).",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StrokePracticeCard(character: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tập viết",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MintLight.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = MintPrimary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                DrawPracticeCanvas(
                    guideCharacter = character,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(
                text = "Vẽ chồng lên sơ đồ thứ tự nét trong ô (số và màu nét).",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun KanjiInfoCard(kanji: Kanji) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MintLight.copy(alpha = 0.35f))
                    .border(
                        width = 1.dp,
                        color = MintPrimary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = kanji.character,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = kanji.meaning,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                InfoLine(label = "Bộ thủ", value = getRadicalForKanji(kanji.character), valueColor = MintPrimary)
                InfoLine(label = "Trình độ", value = "N5")
                InfoLine(label = "Số nét", value = kanji.strokeCount.toString())
                InfoLine(label = "Âm Kun", value = kanji.kunyomi.ifBlank { "-" })
                InfoLine(label = "Âm On", value = kanji.onyomi.ifBlank { "-" })
            }
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.width(70.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExamplesHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Từ vựng có chứa",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "$count từ",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ExampleWordCard(word: Word, onSpeak: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = word.kanji.ifBlank { word.furigana },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (word.kanji.isNotBlank() && word.furigana.isNotBlank() && word.furigana != word.kanji) {
                    Text(
                        text = word.furigana,
                        style = MaterialTheme.typography.bodySmall,
                        color = MintPrimary
                    )
                }
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            JapaneseAudioButton(
                text = word.furigana.ifBlank { word.kanji },
                onSpeak = onSpeak
            )
        }
    }
}

private fun getRadicalForKanji(character: String): String {
    return when (character) {
        "一" -> "Nhất (一)"
        "二" -> "Nhị (二)"
        "三" -> "Nhất (一)"
        "四" -> "Vi (囗)"
        "五" -> "Nhị (二)"
        "六" -> "Bát (八)"
        "七" -> "Nhất (一)"
        "八" -> "Bát (八)"
        "九" -> "Ất (乙)"
        "十" -> "Thập (十)"
        "百" -> "Bạch (白)"
        "千" -> "Thập (十)"
        "万" -> "Nhất (一)"
        "円" -> "Quynh (冂)"
        "日" -> "Nhật (日)"
        "月" -> "Nguyệt (月)"
        "火" -> "Hỏa (火)"
        "水" -> "Thủy (水)"
        "木" -> "Mộc (木)"
        "金" -> "Kim (金)"
        "土" -> "Thổ (土)"
        "山" -> "Sơn (山)"
        "川" -> "Xuyên (川)"
        "田" -> "Điền (田)"
        "人" -> "Nhân (人)"
        "子" -> "Tử (子)"
        "女" -> "Nữ (女)"
        "口" -> "Khẩu (口)"
        "耳" -> "Nhĩ (耳)"
        "手" -> "Thủ (手)"
        "足" -> "Túc (足)"
        "目" -> "Mục (目)"
        "見" -> "Kiến (見)"
        "行" -> "Xích (彳)"
        "来" -> "Nhân (人)"
        "食" -> "Thực (食)"
        "飲" -> "Thực (食)"
        "会" -> "Nhân (人)"
        "書" -> "Viết (曰)"
        "読" -> "Ngôn (言)"
        "話" -> "Ngôn (言)"
        "買" -> "Bối (貝)"
        "上" -> "Nhất (一)"
        "下" -> "Nhất (一)"
        "左" -> "Công (工)"
        "右" -> "Khẩu (口)"
        "中" -> "Cổn (丨)"
        "大" -> "Đại (大)"
        "小" -> "Tiểu (小)"
        "本" -> "Mộc (木)"
        "半" -> "Thập (十)"
        "分" -> "Đao (刀)"
        "何" -> "Nhân (人)"
        "北" -> "Chỉ (匕)"
        "南" -> "Thập (十)"
        "東" -> "Mộc (木)"
        "西" -> "Á (覀)"
        "前" -> "Đao (刀)"
        "後" -> "Xích (彳)"
        "年" -> "Can (干)"
        "午" -> "Thập (十)"
        "時" -> "Nhật (日)"
        "間" -> "Môn (門)"
        "先" -> "Nhi (儿)"
        "生" -> "Sinh (生)"
        "今" -> "Nhân (人)"
        "毎" -> "Vô (毋)"
        "友" -> "Hựu (又)"
        "名" -> "Khẩu (口)"
        "父" -> "Phụ (父)"
        "母" -> "Vô (毋)"
        "高" -> "Cao (高)"
        "安" -> "Miên (宀)"
        "新" -> "Cân (斤)"
        "古" -> "Khẩu (口)"
        "多" -> "Tịch (夕)"
        "少" -> "Tiểu (小)"
        "曜" -> "Nhật (日)"
        "校" -> "Mộc (木)"
        "店" -> "Nghiễm (广)"
        "駅" -> "Mã (馬)"
        "社" -> "Thị (示)"
        "国" -> "Vi (囗)"
        "道" -> "Sước (⻌)"
        "車" -> "Xa (車)"
        "花" -> "Thảo (艹)"
        "雨" -> "Vũ (雨)"
        "空" -> "Huyệt (穴)"
        "天" -> "Đại (大)"
        "気" -> "Vô (气)"
        "電" -> "Vũ (雨)"
        "語" -> "Ngôn (言)"
        "英" -> "Thảo (艹)"
        "外" -> "Tịch (夕)"
        "聞" -> "Môn (門)"
        "休" -> "Nhân (人)"
        "出" -> "Khảm (凵)"
        "入" -> "Nhập (入)"
        "週" -> "Sước (⻌)"
        "白" -> "Bạch (白)"
        "黒" -> "Hắc (黑)"
        "赤" -> "Xích (赤)"
        "青" -> "Thanh (青)"
        "長" -> "Trường (長)"
        "親" -> "Kiến (見)"
        "男" -> "Điền (田)"
        "門" -> "Môn (門)"
        "去" -> "Khư (厶)"
        "銀" -> "Kim (金)"
        "病" -> "Nạch (疒)"
        "院" -> "Phụ (⻖)"
        "医" -> "Phương (匚)"
        "者" -> "Lão (耂)"
        "京" -> "Đầu (亠)"
        "都" -> "Ấp (⻎)"
        "府" -> "Nghiễm (广)"
        "県" -> "Mục (目)"
        "市" -> "Cân (巾)"
        "町" -> "Điền (田)"
        "村" -> "Mộc (木)"
        "区" -> "Phương (匚)"
        "港" -> "Thủy (氵)"
        "紙" -> "Mịch (糸)"
        "歌" -> "Khiếm (欠)"
        "画" -> "Điền (田)"
        "映" -> "Nhật (日)"
        "旅" -> "Phương (方)"
        "館" -> "Thực (食)"
        "物" -> "Ngưu (牛)"
        "鳥" -> "Điểu (鳥)"
        "犬" -> "Khuyển (犬)"
        "猫" -> "Khuyển (犭)"
        "魚" -> "Ngư (魚)"
        "肉" -> "Nhục (肉)"
        "米" -> "Mễ (米)"
        "茶" -> "Thảo (艹)"
        "酒" -> "Thủy (氵)"
        "勉" -> "Lực (力)"
        "強" -> "Cung (弓)"
        "研" -> "Thạch (石)"
        "究" -> "Huyệt (穴)"
        "留" -> "Điền (田)"
        "質" -> "Bối (貝)"
        "題" -> "Hiệp (頁)"
        "答" -> "Trúc (⺮)"
        "試" -> "Ngôn (言)"
        "験" -> "Mã (馬)"
        "意" -> "Tâm (心)"
        "味" -> "Khẩu (口)"
        "授" -> "Thủ (扌)"
        "業" -> "Mộc (木)"
        "世" -> "Nhất (一)"
        "界" -> "Điền (田)"
        "様" -> "Mộc (木)"
        "主" -> "Chủ (丶)"
        "使" -> "Nhân (人)"
        "運" -> "Sước (⻌)"
        "転" -> "Xa (車)"
        "乗" -> "Phiệt (丿)"
        "降" -> "Phụ (⻖)"
        "洗" -> "Thủy (氵)"
        "洋" -> "Thủy (氵)"
        "服" -> "Nguyệt (月)"
        "借" -> "Nhân (人)"
        "貸" -> "Bối (貝)"
        "代" -> "Nhân (人)"
        "思" -> "Tâm (心)"
        "考" -> "Lão (耂)"
        "知" -> "Thỉ (矢)"
        "送" -> "Sước (⻌)"
        "通" -> "Sước (⻌)"
        "止" -> "Chỉ (止)"
        "歩" -> "Chỉ (止)"
        "急" -> "Tâm (心)"
        "待" -> "Xích (彳)"
        "持" -> "Thủ (扌)"
        "立" -> "Lập (立)"
        "作" -> "Nhân (人)"
        "売" -> "Sĩ (士)"
        "言" -> "Ngôn (言)"
        "海" -> "Thủy (氵)"
        "林" -> "Mộc (木)"
        "森" -> "Mộc (木)"
        else -> "Đang cập nhật"
    }
}
