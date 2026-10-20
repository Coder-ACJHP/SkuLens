package com.coder.skulens.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coder.skulens.data.ProductItem
import com.coder.skulens.data.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CatalogViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products: StateFlow<List<ProductItem>> = _products

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedProduct = MutableStateFlow<ProductItem?>(null)
    val selectedProduct: StateFlow<ProductItem?> = _selectedProduct

    val filteredProducts: StateFlow<List<ProductItem>> = combine(_products, _searchQuery) { list, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) list else list.filter { it.sku.contains(trimmed, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val list = repository.getAllProducts()
            _products.value = list
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectProduct(product: ProductItem?) {
        _selectedProduct.value = product
    }

    fun deleteProduct(product: ProductItem) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.deleteProduct(product.id)) {
                _products.value = _products.value.filter { it.id != product.id }
                if (_selectedProduct.value?.id == product.id) {
                    _selectedProduct.value = null
                }
            }
        }
    }
}
