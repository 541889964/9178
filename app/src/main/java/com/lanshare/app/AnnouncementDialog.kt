package com.lanshare.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Announcement(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val content: String,
    val showOnce: Boolean = true,
    val primaryBtn: String = "我知道了",
    val secondaryBtn: String = "",
    val link: String = ""
)

@Composable
fun AnnouncementDialog(
    data: Announcement,
    onDismiss: () -> Unit,
    onDontShowAgain: () -> Unit
) {
    val cardScale = remember { Animatable(0.82f) }
    val cardAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { cardScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
        launch { cardAlpha.animateTo(1f, tween(280)) }
        kotlinx.coroutines.delay(180)
        contentAlpha.animateTo(1f, tween(400))
    }

    fun close(remember: Boolean) {
        if (remember) onDontShowAgain() else onDismiss()
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * cardAlpha.value))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { close(false) }
    )

    Box(
        Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                    alpha = cardAlpha.value
                }
                .shadow(28.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier.fillMaxWidth().graphicsLayer { alpha = contentAlpha.value }
            ) {
                Box(
                    Modifier.fillMaxWidth().height(6.dp).background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFC8281C), Color(0xFFD4A017), Color(0xFFC8281C))
                        )
                    )
                )

                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏮", fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(data.title, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface)
                            if (data.subtitle.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(data.subtitle, fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Box(
                        Modifier.fillMaxWidth().height(1.dp).background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFFD4A017).copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())
                    ) {
                        Text(data.content, fontSize = 14.sp, lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (data.secondaryBtn.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { close(true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(data.secondaryBtn, fontSize = 13.sp) }
                        }
                        Button(
                            onClick = { close(true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(data.primaryBtn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { close(true) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("不再提示", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
