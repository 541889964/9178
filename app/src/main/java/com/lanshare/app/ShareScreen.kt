package com.lanshare.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(vm: ShareViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var showQr by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMsg) {
        snackMsg?.let { snackbarHostState.showSnackbar(it); snackMsg = null }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                title = {
                    Column {
                        Text("🏮 家乡分享", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary)
                        Text(ui.url.ifEmpty { "未启动" }, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("url", ui.url))
                        snackMsg = "地址已复制"
                    }) { Icon(Icons.Outlined.ContentCopy, "复制") }
                    IconButton(onClick = { vm.refreshFiles() }) {
                        Icon(Icons.Outlined.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            StatusCard(ui, onQrClick = { showQr = true })
            Spacer(Modifier.height(18.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📁 文件", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("${ui.files.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                OutlinedTextField(
                    value = searchQuery, onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索…", fontSize = 13.sp) },
                    singleLine = true, modifier = Modifier.width(150.dp),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(16.dp)) }
                )
            }

            Spacer(Modifier.height(12.dp))

            val filtered = remember(ui.files, searchQuery) {
                if (searchQuery.isBlank()) ui.files
                else ui.files.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }

            if (filtered.isEmpty()) EmptyState(searchQuery.isNotBlank())
            else filtered.forEach {
                FileRow(it, onDelete = { vm.deleteFile(it.name) })
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(60.dp))
        }
    }

    if (showQr && ui.url.isNotEmpty()) QrDialog(ui.url) { showQr = false }
}

@Composable
private fun StatusCard(ui: ShareUiState, onQrClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(4.dp).background(
                Brush.horizontalGradient(listOf(Color(0xFFC8281C), Color(0xFFD4A017), Color(0xFFC8281C)))
            ))
            Row(Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                QrThumbnail(ui.url, Modifier.size(92.dp), onClick = onQrClick)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(
                            if (ui.running) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error))
                        Spacer(Modifier.width(8.dp))
                        Text(if (ui.running) "服务运行中" else "服务已停止",
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(ui.url.ifEmpty { "等待网络…" }, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                    Text("扫码或输入地址 · ${ui.files.size} 个文件",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun QrThumbnail(url: String, size: Modifier, onClick: () -> Unit) {
    var bmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        if (url.isEmpty()) return@LaunchedEffect
        bmp = withContext(Dispatchers.Default) { genQr(url, 400, 1) }
    }
    Box(size.clip(RoundedCornerShape(12.dp)).background(Color.White)
        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        .clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        bmp?.let { Image(it.asImageBitmap(), "二维码",
            modifier = Modifier.fillMaxSize().padding(6.dp)) }
            ?: CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun FileRow(file: FileItem, onDelete: () -> Unit) {
    val icon = when {
        file.name.endsWith(".jpg", true) || file.name.endsWith(".png", true) ||
        file.name.endsWith(".jpeg", true) || file.name.endsWith(".webp", true) -> "🖼️"
        file.name.endsWith(".mp4", true) || file.name.endsWith(".mkv", true) -> "🎬"
        file.name.endsWith(".mp3", true) || file.name.endsWith(".wav", true) -> "🎵"
        file.name.endsWith(".pdf", true) -> "📄"
        file.name.endsWith(".zip", true) || file.name.endsWith(".rar", true) -> "📦"
        else -> "📎"
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) { Text(icon, fontSize = 22.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text("${formatSize(file.size)} · ${formatTime(file.mtime)}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (searching) "🔍" else "📂", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(if (searching) "没有匹配的文件" else "还没有文件\n从电脑浏览器上传试试",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun QrDialog(url: String, onDismiss: () -> Unit) {
    var bmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) { bmp = withContext(Dispatchers.Default) { genQr(url, 800, 2) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        title = { Text("🏮 扫码访问", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                bmp?.let {
                    Image(it.asImageBitmap(), null,
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)))
                } ?: CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(url, fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    )
}

private fun genQr(text: String, size: Int, margin: Int): android.graphics.Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to margin)
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = android.graphics.Bitmap.createBitmap(m.width, m.height,
            android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until m.width) for (y in 0 until m.height)
            bmp.setPixel(x, y, if (m[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        bmp
    } catch (_: Exception) { null }
}

private fun formatSize(n: Long): String {
    if (n < 1024) return "$n B"
    val u = arrayOf("KB", "MB", "GB", "TB")
    var v = n.toDouble(); var i = -1
    do { v /= 1024; i++ } while (v >= 1024 && i < u.size - 1)
    return if (v >= 100) "${v.toInt()} ${u[i]}" else String.format("%.1f %s", v, u[i])
}

private fun formatTime(ms: Long): String {
    val d = (System.currentTimeMillis() - ms) / 1000
    return when {
        d < 60 -> "刚刚"
        d < 3600 -> "${d / 60} 分钟前"
        d < 86400 -> "${d / 3600} 小时前"
        d < 604800 -> "${d / 86400} 天前"
        else -> java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(ms))
    }
}
