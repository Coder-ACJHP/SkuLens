package com.coder.skulens.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.coder.skulens.App
import com.coder.skulens.ui.components.AddTab
import com.coder.skulens.ui.components.CatalogTab
import com.coder.skulens.ui.components.ModernFloatingTabBar
import com.coder.skulens.ui.components.SearchTab
import com.coder.skulens.ui.components.SettingsScreen
import com.coder.skulens.ui.components.TopHeader
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

    BackHandler(enabled = selectedTab != 0) {
        viewModel.selectTab(0)
    }

    val topBarTitle = when (selectedTab) {
        0 -> "Sirius Lens"
        1 -> "Ürün Ekle"
        2 -> "Kayıtlar"
        3 -> "Ayarlar"
        else -> "Sirius Lens"
    }

    Scaffold(
        containerColor = Surface,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopHeader(title = topBarTitle)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Ekran içeriği alta kadar uzanır ve tabbar altına kayabilir (iOS 17/18 stili)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> SearchTab(viewModel)
                    1 -> AddTab(viewModel)
                    2 -> CatalogTab(
                        viewModel = catalogViewModel,
                        onNavigateToAddTab = { viewModel.selectTab(1) }
                    )
                    3 -> SettingsScreen()
                }
            }

            // Tabbar ekranın üzerinde havada (floating overlay) durur, arkasında opak blok yoktur
            ModernFloatingTabBar(
                selectedTab = selectedTab,
                onTabSelected = viewModel::selectTab,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
