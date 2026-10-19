package com.coder.skulens.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.coder.skulens.ui.components.AddTab
import com.coder.skulens.ui.components.SearchTab
import com.coder.skulens.ui.components.TopHeaderAndNav
import com.coder.skulens.ui.theme.Surface

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        containerColor = Surface,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopHeaderAndNav(
                selectedTab = selectedTab,
                onTabSelected = viewModel::selectTab
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .navigationBarsPadding()
                .fillMaxSize()
        ) {
            if (selectedTab == 0) {
                SearchTab(viewModel)
            } else {
                AddTab(viewModel)
            }
        }
    }
}
