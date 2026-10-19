package com.coder.skulens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.skulens.ui.theme.OnSecondaryContainer
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.SecondaryContainer
import com.coder.skulens.ui.theme.SurfaceContainerHighest
import com.coder.skulens.ui.theme.SurfaceContainerLow

@Composable
fun OfflineAssuranceCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Tüm işlemler internet olmadan, cihaz içi yapay zeka ile çalışır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(SecondaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "%100 Çevrimdışı",
                color = OnSecondaryContainer,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
