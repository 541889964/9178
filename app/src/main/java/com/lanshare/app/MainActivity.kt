package com.lanshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

private val LightColors = lightColorScheme(
    primary = Color(0xFFC8281C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5E0),
    secondary = Color(0xFFD4A017),
    background = Color(0xFFFFF8F0),
    surface = Color(0xFFFFFCF7),
    surfaceVariant = Color(0xFFFFEFE5),
    onSurface = Color(0xFF2A1F18),
    onSurfaceVariant = Color(0xFF7A6357),
    outline = Color(0xFFE8D5C4),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B5B),
    onPrimary = Color(0xFF2A0B08),
    primaryContainer = Color(0xFF5C1A14),
    background = Color(0xFF140D0A),
    surface = Color(0xFF1F1613),
    surfaceVariant = Color(0xFF2A1F1B),
    onSurface = Color(0xFFF5E6DC),
    onSurfaceVariant = Color(0xFFB8A296),
    outline = Color(0xFF3D2A24),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
                LanShareApp()
            }
        }
    }
}

enum class AppStage { SPLASH, MAIN }

@Composable
fun LanShareApp() {
    val vm: ShareViewModel = viewModel()
    var stage by remember { mutableStateOf(AppStage.SPLASH) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }

    WallpaperBackground()

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            (fadeIn(tween(500)) + scaleIn(initialScale = 1.05f, animationSpec = tween(500)))
                .togetherWith(fadeOut(tween(400)) + scaleOut(targetScale = 1.02f, animationSpec = tween(400)))
        },
        label = "stage"
    ) { s ->
        when (s) {
            AppStage.SPLASH -> SplashScreen { stage = AppStage.MAIN }
            AppStage.MAIN -> {
                LaunchedEffect(Unit) {
                    delay(700)
                    announcement = vm.loadAnnouncement()
                }
                ShareScreen(vm)
            }
        }
    }

    AnimatedVisibility(
        visible = announcement != null,
        enter = fadeIn(tween(300)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(tween(260)) + scaleOut(targetScale = 0.9f, animationSpec = tween(260))
    ) {
        announcement?.let { a ->
            AnnouncementDialog(
                data = a,
                onDismiss = { announcement = null },
                onDontShowAgain = {
                    vm.markAnnouncementRead(a.id)
                    announcement = null
                }
            )
        }
    }
}

@Composable
private fun WallpaperBackground() {
    val ctx = LocalContext.current
    var bmp by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(Unit) {
        bmp = with(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val names = ctx.assets.list("wallpapers")?.sorted() ?: return@with null
                if (names.isEmpty()) return@with null
                ctx.assets.open("wallpapers/${names.first()}").use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
            } catch (_: Exception) { null }
        }
    }
    Box(Modifier.fillMaxSize()) {
        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.32f
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                    )
                )
            )
        )
    }
}
