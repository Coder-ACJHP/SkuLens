package com.coder.skulens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coder.skulens.ui.theme.CodeSku
import com.coder.skulens.ui.theme.Primary
import com.coder.skulens.ui.theme.PrimaryFixed
import com.coder.skulens.ui.theme.Secondary
import com.coder.skulens.ui.theme.SurfaceContainer
import com.coder.skulens.ui.theme.SurfaceContainerHighest
import com.coder.skulens.ui.theme.SurfaceContainerLowest

@Composable
fun SkuInputCard(
    skuInput: String,
    confirmedSku: String,
    isConfirmed: Boolean,
    onSkuInputChange: (String) -> Unit,
    onClearInput: () -> Unit,
    onConfirmSku: () -> Boolean,
    onEditSku: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Yeni Ürün Eşleme",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isConfirmed) PrimaryFixed else SurfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isConfirmed) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Onaylandı",
                            color = Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Secondary)
                        )
                        Text(
                            text = "Giriş Bekleniyor",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (isConfirmed) {
                // Confirmed SKU View (Stable, user can copy or edit)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContainerLowest)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = confirmedSku,
                            style = CodeSku,
                            color = Primary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(confirmedSku)) }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopyala",
                                tint = Secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onEditSku) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                // Input TextField with Done keyboard action
                OutlinedTextField(
                    value = skuInput,
                    onValueChange = onSkuInputChange,
                    textStyle = CodeSku,
                    placeholder = {
                        Text(
                            text = "örn. 8690123456789",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row {
                            if (skuInput.isNotEmpty()) {
                                IconButton(onClick = onClearInput) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Temizle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (onConfirmSku()) {
                                            keyboardController?.hide()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Onayla",
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (onConfirmSku()) {
                                keyboardController?.hide()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceContainerLowest,
                        unfocusedContainerColor = SurfaceContainerLowest,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Subtitle Helper Prompt
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Secondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isConfirmed) "SKU onaylandı. Ürün fotoğrafı çekin veya galeriden seçin." else "SKU yazıp klavyeden 'Bitti' veya ✓ butonuna basın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
