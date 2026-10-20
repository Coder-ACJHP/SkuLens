package com.coder.skulens.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coder.skulens.data.ProductItem
import com.coder.skulens.data.ProductRepository
import com.coder.skulens.data.SaveProductResult
import com.coder.skulens.data.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val _duplicateProduct = MutableStateFlow<ProductItem?>(null)
    val duplicateProduct: StateFlow<ProductItem?> = _duplicateProduct

    private val _stagedBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val stagedBitmaps: StateFlow<List<Bitmap>> = _stagedBitmaps

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private var checkSkuJob: Job? = null

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun onSkuInputChange(value: String) {
        _skuInput.value = value.uppercase()
        if (_duplicateProduct.value != null) {
            _duplicateProduct.value = null
        }
    }

    fun clearSkuInput() {
        _skuInput.value = ""
        _duplicateProduct.value = null
    }

    fun confirmSku(): Boolean {
        val trimmed = _skuInput.value.trim().uppercase()
        if (trimmed.isBlank()) return false

        _duplicateProduct.value = null
        checkSkuJob?.cancel()
        checkSkuJob = viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.findProductBySku(trimmed)
            if (existing != null) {
                _duplicateProduct.value = existing
            } else {
                _stagedBitmaps.value = emptyList()
                _confirmedSku.value = trimmed
            }
        }
        return true
    }

    fun editSku() {
        _confirmedSku.value = ""
        _stagedBitmaps.value = emptyList()
        _duplicateProduct.value = null
    }

    fun addStagedAngle(bitmap: Bitmap) {
        if (_stagedBitmaps.value.size < ProductRepository.MAX_ANGLES_PER_SKU) {
            _stagedBitmaps.value = _stagedBitmaps.value + bitmap
        }
    }

    fun addStagedAngle(uri: Uri?) {
        if (uri == null) return
        if (_stagedBitmaps.value.size >= ProductRepository.MAX_ANGLES_PER_SKU) return
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = repository.decodeBitmapFromUri(uri)
            if (bitmap != null) {
                _stagedBitmaps.value = _stagedBitmaps.value + bitmap
            }
        }
    }

    fun removeStagedAngle(index: Int) {
        val current = _stagedBitmaps.value.toMutableList()
        if (index in current.indices) {
            val removed = current.removeAt(index)
            if (!removed.isRecycled) {
                removed.recycle()
            }
            _stagedBitmaps.value = current
        }
    }

    fun saveStagedProduct() {
        val sku = _confirmedSku.value.trim().uppercase()
        val bitmaps = _stagedBitmaps.value
        if (sku.isBlank() || bitmaps.isEmpty()) return

        runSave(sku) {
            repository.saveProduct(sku, bitmaps)
        }
    }

    fun startNewSearch() {
        _isCapturing.value = true
        _searchState.value = SearchState.Idle
    }

    fun dismissSearchResult() {
        _searchState.value = SearchState.Idle
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
            runSearch { repository.searchProduct(uri) }
        }
    }

    fun dismissDuplicateProduct() {
        _duplicateProduct.value = null
        _confirmedSku.value = ""
        _stagedBitmaps.value = emptyList()
    }

    fun resetAddState() {
        _skuInput.value = ""
        _confirmedSku.value = ""
        _stagedBitmaps.value = emptyList()
        _duplicateProduct.value = null
    }

    private fun runSave(sku: String, block: () -> SaveProductResult) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            val startTime = System.currentTimeMillis()
            var savedSuccessfully = false
            var duplicateProduct: ProductItem? = null
            var feedbackMessage: String? = null

            try {
                when (val result = block()) {
                    is SaveProductResult.Success -> {
                        savedSuccessfully = true
                        feedbackMessage = "Kaydedildi: $sku (${result.newCount} açı ile)"
                    }
                    is SaveProductResult.AlreadyExists -> {
                        duplicateProduct = result.existingProduct
                    }
                    is SaveProductResult.Failure -> {
                        feedbackMessage = result.message ?: "Görsel işlenemedi"
                    }
                }
            } catch (e: Exception) {
                feedbackMessage = "Hata: ${e.message}"
            }

            val elapsed = System.currentTimeMillis() - startTime
            val remaining = MIN_LOADING_DURATION_MS - elapsed
            if (remaining > 0) {
                delay(remaining)
            }

            if (savedSuccessfully) {
                _skuInput.value = ""
                _confirmedSku.value = ""
                _stagedBitmaps.value = emptyList()
                _duplicateProduct.value = null
                _message.value = feedbackMessage
            } else if (duplicateProduct != null) {
                _confirmedSku.value = ""
                _stagedBitmaps.value = emptyList()
                _message.value = null
                _duplicateProduct.value = duplicateProduct
            } else {
                _message.value = feedbackMessage
            }
            _isSaving.value = false
        }
    }

    private fun runSearch(block: () -> List<SearchResult>) {
        viewModelScope.launch(Dispatchers.IO) {
            _searchState.value = SearchState.Loading
            val startTime = System.currentTimeMillis()
            val result = try {
                val results = block()
                if (results.isEmpty()) SearchState.NoMatch else SearchState.Found(results)
            } catch (e: Exception) {
                SearchState.Error(e.message ?: "Bilinmeyen hata")
            }
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = MIN_LOADING_DURATION_MS - elapsed
            if (remaining > 0) {
                delay(remaining)
            }
            _searchState.value = result
        }
    }

    fun clearMessage() { _message.value = null }

    companion object {
        private const val MIN_LOADING_DURATION_MS = 1000L
    }
}
