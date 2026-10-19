package com.coder.skulens.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coder.skulens.data.ProductRepository
import com.coder.skulens.data.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _skuInput = MutableStateFlow("")
    val skuInput: StateFlow<String> = _skuInput

    private val _confirmedSku = MutableStateFlow("")
    val confirmedSku: StateFlow<String> = _confirmedSku

    private val _isCapturing = MutableStateFlow(true)
    val isCapturing: StateFlow<Boolean> = _isCapturing

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun onSkuInputChange(value: String) {
        _skuInput.value = value
    }

    fun clearSkuInput() {
        _skuInput.value = ""
    }

    fun confirmSku(): Boolean {
        val trimmed = _skuInput.value.trim()
        if (trimmed.isNotBlank()) {
            _confirmedSku.value = trimmed
            return true
        }
        return false
    }

    fun editSku() {
        _confirmedSku.value = ""
    }

    fun startNewSearch() {
        _isCapturing.value = true
    }

    fun isSuccessMessage(msg: String): Boolean =
        !msg.startsWith("Hata") && !msg.contains("işlenemedi")

    // Kamera kaynağı (bitmap zaten ≤512px gelir)
    fun saveNewProduct(bitmap: Bitmap) {
        val sku = _confirmedSku.value
        if (sku.isNotBlank()) {
            saveNewProduct(sku, bitmap)
        }
    }

    fun saveNewProduct(sku: String, bitmap: Bitmap) =
        runSave(sku) { repository.saveProduct(sku, bitmap) }

    fun searchSimilarProducts(bitmap: Bitmap) {
        _isCapturing.value = false
        runSearch { repository.searchProduct(bitmap) }
    }

    // Galeri kaynağı (decode + EXIF + 512px küçültme IO thread'inde repository içinde yapılır)
    fun saveNewProduct(uri: Uri?) {
        val sku = _confirmedSku.value
        if (uri != null && sku.isNotBlank()) {
            saveNewProduct(sku, uri)
        }
    }

    fun saveNewProduct(sku: String, uri: Uri) =
        runSave(sku) { repository.saveProduct(sku, uri) }

    fun searchSimilarProducts(uri: Uri?) {
        if (uri != null) {
            _isCapturing.value = false
            runSearch { repository.searchProduct(uri) }
        }
    }

    private fun runSave(sku: String, block: () -> Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _message.value = try {
                if (block()) "Kaydedildi: $sku" else "Görsel işlenemedi"
            } catch (e: Exception) {
                "Hata: ${e.message}"
            }
        }
    }

    private fun runSearch(block: () -> List<SearchResult>) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchState.value = SearchState.Loading
            _searchState.value = try {
                val results = block()
                if (results.isEmpty()) SearchState.NoMatch else SearchState.Found(results)
            } catch (e: Exception) {
                SearchState.Error(e.message ?: "Bilinmeyen hata")
            }
        }
    }

    fun clearMessage() { _message.value = null }
}
