package com.coder.skulens.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coder.skulens.data.ProductItem
import com.coder.skulens.data.ProductRepository
import com.coder.skulens.data.SaveProductResult
import com.coder.skulens.data.SearchResult
import com.coder.skulens.engine.DetectedGarment
import com.coder.skulens.engine.ImagePreprocessor
import com.coder.skulens.ui.components.MultiDetectionState
import com.coder.skulens.util.AppLogger
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

    private val _multiDetectionState = MutableStateFlow<MultiDetectionState?>(null)
    val multiDetectionState: StateFlow<MultiDetectionState?> = _multiDetectionState

    private val _isDetecting = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting

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
        _multiDetectionState.value = null
    }

    fun addStagedAngle(bitmap: Bitmap) {
        val remaining = ProductRepository.MAX_ANGLES_PER_SKU - _stagedBitmaps.value.size
        if (remaining <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            _isDetecting.value = true
            try {
                val detected = repository.detectGarments(bitmap)
                if (detected.size > 1) {
                    _multiDetectionState.value = MultiDetectionState(
                        sourceBitmap = bitmap,
                        garments = detected,
                        maxSelectable = remaining
                    )
                } else if (detected.size == 1) {
                    val crop = detected[0].crop
                    _stagedBitmaps.value = _stagedBitmaps.value + crop
                } else {
                    val prepared = ImagePreprocessor.resize(bitmap)
                    _stagedBitmaps.value = _stagedBitmaps.value + prepared
                }
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "Detection error in addStagedAngle: ${e.message}", e)
                val prepared = ImagePreprocessor.resize(bitmap)
                _stagedBitmaps.value = _stagedBitmaps.value + prepared
            } finally {
                _isDetecting.value = false
            }
        }
    }

    fun addStagedAngle(uri: Uri?) {
        if (uri == null) return
        val remaining = ProductRepository.MAX_ANGLES_PER_SKU - _stagedBitmaps.value.size
        if (remaining <= 0) return

        viewModelScope.launch(Dispatchers.IO) {
            _isDetecting.value = true
            try {
                val bitmap = repository.decodeBitmapFromUri(uri) ?: return@launch
                val detected = repository.detectGarments(bitmap)
                if (detected.size > 1) {
                    _multiDetectionState.value = MultiDetectionState(
                        sourceBitmap = bitmap,
                        garments = detected,
                        maxSelectable = remaining
                    )
                } else if (detected.size == 1) {
                    val crop = detected[0].crop
                    _stagedBitmaps.value = _stagedBitmaps.value + crop
                } else {
                    val prepared = ImagePreprocessor.resize(bitmap)
                    _stagedBitmaps.value = _stagedBitmaps.value + prepared
                }
            } catch (e: Exception) {
                AppLogger.e("MainViewModel", "Detection error in gallery addStagedAngle: ${e.message}", e)
            } finally {
                _isDetecting.value = false
            }
        }
    }

    fun confirmMultiGarmentSelection(selected: List<DetectedGarment>) {
        val remaining = ProductRepository.MAX_ANGLES_PER_SKU - _stagedBitmaps.value.size
        val toAdd = selected.take(remaining).map { it.crop }
        if (toAdd.isNotEmpty()) {
            _stagedBitmaps.value = _stagedBitmaps.value + toAdd
        }
        _multiDetectionState.value = null
    }

    fun dismissMultiGarmentSelection() {
        _multiDetectionState.value = null
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

    fun searchSimilarProducts(bitmap: Bitmap) {
        runSearch { repository.searchProduct(bitmap) }
    }

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
