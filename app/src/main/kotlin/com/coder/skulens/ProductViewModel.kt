package com.coder.skulens

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Found(val results: List<SearchResult>) : SearchState
    data object NoMatch : SearchState
    data class Error(val message: String) : SearchState
}

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // Kamera kaynağı (bitmap zaten ≤512px gelir)
    fun saveNewProduct(sku: String, bitmap: Bitmap) =
        runSave(sku) { repository.saveProduct(sku, bitmap) }

    fun searchSimilarProducts(bitmap: Bitmap) =
        runSearch { repository.searchProduct(bitmap) }

    // Galeri kaynağı (decode + EXIF + 512px küçültme IO thread'inde repository içinde yapılır)
    fun saveNewProduct(sku: String, uri: Uri) =
        runSave(sku) { repository.saveProduct(sku, uri) }

    fun searchSimilarProducts(uri: Uri) =
        runSearch { repository.searchProduct(uri) }

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
