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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.coder.skulens.App
import com.coder.skulens.ui.components.AddTab
import com.coder.skulens.ui.components.CatalogTab
import com.coder.skulens.ui.components.SearchTab
import com.coder.skulens.ui.components.TopHeaderAndNav
import com.coder.skulens.ui.theme.Surface

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    catalogViewModel: CatalogViewModel = run {
        val context = LocalContext.current
        viewModel(
            factory = viewModelFactory {
                initializer {
                    val app = context.applicationContext as App
                    CatalogViewModel(app.repository)
                }
            }
        )
    }
) {
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
            when (selectedTab) {
                0 -> SearchTab(viewModel)
                1 -> AddTab(viewModel)
                else -> CatalogTab(
                    viewModel = catalogViewModel,
                    onNavigateToAddTab = { viewModel.selectTab(1) }
                )
            }
        }
    }
}
