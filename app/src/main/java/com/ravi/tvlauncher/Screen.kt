package com.ravi.tvlauncher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DefaultTint = Color(0xFF1B2A49)
private val TileColor = Color(0xFF1C1C1E)
private val TileShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LauncherScreen(
    apps: List<AppItem>,
    hiddenCount: Int,
    homeSignal: Int,
    onLaunch: (AppItem) -> Unit,
    onHide: (AppItem) -> Unit,
    onUnhideAll: () -> Unit,
    onSettings: () -> Unit,
) {
    // A launcher must never exit on Back.
    BackHandler(enabled = true) { }

    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }
    var focusLabel by remember { mutableStateOf("") }
    var focusTint by remember { mutableStateOf(DefaultTint) }
    val tint by animateColorAsState(focusTint, tween(700), label = "tint")

    // Focus the first app when the list appears or changes size (e.g. after hiding one).
    LaunchedEffect(apps.size) {
        if (apps.isNotEmpty()) {
            delay(100)
            runCatching { firstFocus.requestFocus() }
        }
    }
    // Home pressed while already here -> jump back to the start.
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            listState.animateScrollToItem(0)
            runCatching { firstFocus.requestFocus() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tint, Color(0xFF050507), Color.Black)))
    ) {
        Column(Modifier.fillMaxSize().padding(vertical = 36.dp)) {

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text("Home", fontSize = 22.sp, color = Color.White.copy(alpha = 0.6f))
                Clock()
            }

            Spacer(Modifier.weight(1f))

            Column(Modifier.padding(horizontal = 56.dp)) {
                Text(
                    focusLabel,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Hold OK on an app to hide it",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.45f),
                )
            }

            Spacer(Modifier.height(16.dp))

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 56.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app ->
                    AppTile(
                        app = app,
                        modifier = if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        onFocused = { focusLabel = app.label; focusTint = app.tint },
                        onClick = { onLaunch(app) },
                        onLongClick = { onHide(app) },
                    )
                }
                item(key = "settings") {
                    InfoTile(
                        text = "Settings",
                        onFocused = { focusLabel = "Settings"; focusTint = DefaultTint },
                        onClick = onSettings,
                    )
                }
                if (hiddenCount > 0) {
                    item(key = "unhide") {
                        InfoTile(
                            text = "Show $hiddenCount hidden",
                            onFocused = { focusLabel = "Hidden apps"; focusTint = DefaultTint },
                            onClick = onUnhideAll,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AppTile(
    app: AppItem,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .size(280.dp, 158.dp)
            .onFocusChanged { if (it.hasFocus) onFocused() },
        shape = CardDefaults.shape(TileShape),
        scale = CardDefaults.scale(focusedScale = 1.14f),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = TileShape)
        ),
        colors = CardDefaults.colors(containerColor = TileColor, focusedContainerColor = TileColor),
    ) {
        Image(
            bitmap = app.image,
            contentDescription = app.label,
            modifier = if (app.isBanner) Modifier.fillMaxSize() else Modifier.fillMaxSize().padding(34.dp),
            contentScale = if (app.isBanner) ContentScale.Crop else ContentScale.Fit,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun InfoTile(text: String, onFocused: () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .size(280.dp, 158.dp)
            .onFocusChanged { if (it.hasFocus) onFocused() },
        shape = CardDefaults.shape(TileShape),
        scale = CardDefaults.scale(focusedScale = 1.14f),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = TileShape)
        ),
        colors = CardDefaults.colors(containerColor = TileColor, focusedContainerColor = TileColor),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text, fontSize = 24.sp, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Clock() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }
    val minute = now / 60_000
    val time = remember(minute) { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now)) }
    val date = remember(minute) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)) }

    Column(horizontalAlignment = Alignment.End) {
        Text(time, fontSize = 40.sp, fontWeight = FontWeight.Light, color = Color.White)
        Text(date, fontSize = 18.sp, color = Color.White.copy(alpha = 0.6f))
    }
}
