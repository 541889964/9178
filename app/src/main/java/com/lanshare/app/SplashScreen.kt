package com.lanshare.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val ctx = LocalContext.current
    var logo by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(Unit) {
        logo = with(Dispatchers.IO) {
            try {
                val names = ctx.assets.list("wallpapers")?.sorted() ?: return@with null
                if (names.isEmpty()) return@with null
                ctx.assets.open("wallpapers/${names.first()}").use {
                    android.graphics.BitmapFactory.decodeStream(it)
                }
            } catch (_: Exception) { null }
        }
    }

    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoRot = remember { Animatable(-18f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(30f) }
    val subAlpha = remember { Animatable(0f) }

    val shimmer = rememberInfiniteTransition(label = "sh")
    val shimmerX by shimmer.animateFloat(
        -0.4f, 1.4f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "x"
    )
    val glowPulse by shimmer.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "g"
    )

    val fullSub = "· 家乡 · 分享 ·"
    var typed by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(700)) }
        launch { logoScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        launch { logoRot.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)) }
        delay(280)
        launch { titleAlpha.animateTo(1f, tween(600)) }
        launch { titleOffset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
        delay(300)
        launch { subAlpha.animateTo(1f, tween(500)) }
        for (i in fullSub.indices) {
            typed = fullSub.substring(0, i + 1)
            delay(70)
        }
        delay(700)
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    MaterialTheme.colorScheme.background
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(280.dp)
                .scale(logoScale.value * glowPulse)
                .alpha(0.28f * logoAlpha.value)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        rotationZ = logoRot.value
                        alpha = logoAlpha.value
                    }
                    .shadow(24.dp, RoundedCornerShape(36.dp))
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color.White)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                logo?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(36.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                            start = Offset(shimmerX * 400f, -100f),
                            end = Offset(shimmerX * 400f + 80f, 400f)
                        )
                    )
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "家乡分享",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer { alpha = titleAlpha.value }
                    .offset(y = titleOffset.value.dp),
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                typed + if (typed.length < fullSub.length) "▎" else "",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 3.sp,
                modifier = Modifier.graphicsLayer { alpha = subAlpha.value },
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(60.dp))

            Text(
                "🏮",
                fontSize = 40.sp,
                modifier = Modifier.alpha(subAlpha.value * 0.7f)
            )
        }
    }
}
