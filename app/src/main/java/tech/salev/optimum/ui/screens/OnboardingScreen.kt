package tech.salev.optimum.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Data ──────────────────────────────────────────────────────────────────────

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val detail: String
)

private val PAGES = listOf(
    OnboardingPage(
        icon = Icons.Default.AccessTime,
        title = "Zamanını Takip Et",
        subtitle = "Her 15–30 dakikada bir hatırlatıcı",
        detail = "Optimum, gününü küçük zaman dilimlerine bölerek ne yaptığını kayıt altına almanı sağlar. Böylece zamanının nereye gittiğini tam olarak görürsün."
    ),
    OnboardingPage(
        icon = Icons.Default.Category,
        title = "Kategoriler & Aktiviteler",
        subtitle = "Kendi sistemini kur",
        detail = "İş, spor, okuma, sosyal vakit… Sana anlamlı gelen kategorileri ve altındaki aktiviteleri kendin tanımlarsın."
    ),
    OnboardingPage(
        icon = Icons.Default.TrackChanges,
        title = "Analitik & Analiz",
        subtitle = "Günlük verimlilik skoru",
        detail = "Her gün sonunda günü puanla, günlük not tut. Haftalık ve aylık grafikler üretimliliğini ve alışkanlıklarını yansıtır."
    ),
    OnboardingPage(
        icon = Icons.Default.Notifications,
        title = "Akıllı Hatırlatıcılar",
        subtitle = "Anlık kayıt, hiç kaçırmadan",
        detail = "Bildirimden doğrudan son bloğunu kaydet ya da ertelemek için Snooze'a bas. Uygulamayı açmadan hızlı kayıt yapabilirsin."
    )
)

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * Full-screen onboarding flow shown only on first launch.
 * Calls [onFinish] when the user completes or skips it.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = PAGES[pageIndex]
    val isLastPage = pageIndex == PAGES.lastIndex

    val progressAnim by animateFloatAsState(
        targetValue = (pageIndex + 1f) / PAGES.size,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "onboarding_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Skip button top-right (hidden on last page)
        if (!isLastPage) {
            TextButton(
                onClick = onFinish,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Text(
                    text = "Atla",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Animated page content
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInHorizontally { it / 4 },
                exit = fadeOut() + slideOutHorizontally { -it / 4 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = page.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Bottom bar: progress + CTA button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PAGES.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (i == pageIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .size(if (i == pageIndex) 10.dp else 6.dp)
                    )
                }
            }

            // CTA Button
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        pageIndex++
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isLastPage) "Başlayalım 🚀" else "Devam →",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
