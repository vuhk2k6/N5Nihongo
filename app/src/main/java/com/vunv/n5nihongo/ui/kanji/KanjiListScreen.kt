package com.vunv.n5nihongo.ui.kanji

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vunv.n5nihongo.data.model.Kanji
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import com.vunv.n5nihongo.ui.components.StrokeRulesDialog
import com.vunv.n5nihongo.ui.theme.MintLight
import com.vunv.n5nihongo.ui.theme.MintPrimary
import com.vunv.n5nihongo.ui.theme.SurfaceWhite
import com.vunv.n5nihongo.ui.theme.TextPrimary
import com.vunv.n5nihongo.ui.theme.TextSecondary

@Composable
fun KanjiListRoute(
    viewModel: KanjiListViewModel = viewModel(factory = KanjiListViewModel.factory()),
    onKanjiClick: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    KanjiListScreen(uiState = uiState, onKanjiClick = onKanjiClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanjiListScreen(
    uiState: KanjiListUiState,
    onKanjiClick: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showStrokeRules by remember { mutableStateOf(false) }

    if (showStrokeRules) {
        StrokeRulesDialog(onDismissRequest = { showStrokeRules = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kanji N5",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showStrokeRules = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Quy tắc nét",
                            tint = MintPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MintLight.copy(alpha = 0.18f))
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MintPrimary)
                    }
                }
                uiState.kanjiList.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Chưa có dữ liệu Kanji", color = TextSecondary)
                    }
                }
                else -> KanjiGrid(
                    all = uiState.kanjiList,
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onKanjiClick = onKanjiClick
                )
            }
        }
    }
}

@Composable
private fun KanjiGrid(
    all: List<Kanji>,
    query: String,
    onQueryChange: (String) -> Unit,
    onKanjiClick: (Int) -> Unit
) {
    val filtered = remember(all, query) {
        if (query.isBlank()) {
            all
        } else {
            all.filter { kanji ->
                kanji.character.contains(query, ignoreCase = true) ||
                    kanji.meaning.contains(query, ignoreCase = true) ||
                    kanji.kunyomi.contains(query, ignoreCase = true) ||
                    kanji.onyomi.contains(query, ignoreCase = true)
            }
        }
    }

    LazyVerticalGrid(
        // Adaptive sizing: 5–7 cells/row on phones, more on tablets — keeps the grid dense like
        // the reference design while staying readable on small screens.
        columns = GridCells.Adaptive(minSize = 60.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchAndStats(query = query, onQueryChange = onQueryChange, total = filtered.size)
        }
        items(filtered, key = { it.id }) { kanji ->
            KanjiGridCell(kanji = kanji, onClick = { onKanjiClick(kanji.id) })
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SearchAndStats(
    query: String,
    onQueryChange: (String) -> Unit,
    total: Int
) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Tìm kanji, nghĩa, âm Kun/On…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MintPrimary,
                unfocusedBorderColor = MintPrimary.copy(alpha = 0.5f),
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite
            )
        )
        Text(
            text = "$total chữ Hán N5 — chạm vào ô để xem chi tiết",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun KanjiGridCell(kanji: Kanji, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MintLight.copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                color = MintPrimary.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = kanji.character,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}
