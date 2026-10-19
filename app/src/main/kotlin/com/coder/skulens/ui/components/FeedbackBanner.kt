package com.coder.skulens.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.SuccessContainer
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FeedbackBanner(
    message: String?,
    isSuccess: Boolean,
    onClearMessage: () -> Unit
) {
    AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
        message?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSuccess) SuccessContainer else MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = msg,
                    color = if (isSuccess) Primary else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LaunchedEffect(msg) {
                delay(3000.milliseconds)
                onClearMessage()
            }
        }
    }
}
