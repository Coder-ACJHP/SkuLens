package com.coder.skulens.ui

import com.coder.skulens.data.SearchResult

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Found(val results: List<SearchResult>) : SearchState
    data object NoMatch : SearchState
    data class Error(val message: String) : SearchState
}
