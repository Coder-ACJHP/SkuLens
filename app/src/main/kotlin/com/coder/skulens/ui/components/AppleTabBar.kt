package com.coder.skulens.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Apple HIG TabBar component (SRP: Apple TabBar delegator/alias)
 */
@Composable
fun AppleTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) = ModernFloatingTabBar(selectedTab, onTabSelected, modifier)
