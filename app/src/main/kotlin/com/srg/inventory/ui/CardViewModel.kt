package com.srg.inventory.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srg.inventory.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing card operations and UI state
 */
class CardViewModel(application: Application) : AndroidViewModel(application) {

    private val userCardDatabase = UserCardDatabase.getDatabase(application)
    private val repository = CardRepository(
        userCardDatabase.userCardDao()
    )

    // User collection flows
    val allUserCards: StateFlow<List<UserCard>> = repository.getAllUserCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ownedCards: StateFlow<List<UserCard>> = repository.getOwnedCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wantedCards: StateFlow<List<UserCard>> = repository.getWantedCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    /**
     * Add card to collection
     */
    fun addToCollection(cardId: String, cardName: String, isCustom: Boolean = false) {
        viewModelScope.launch {
            val existing = repository.getUserCard(cardId)
            if (existing != null) {
                // Increment owned count
                repository.addOrUpdateCard(
                    existing.copy(quantityOwned = existing.quantityOwned + 1)
                )
            } else {
                // Create new entry
                repository.addOrUpdateCard(
                    UserCard(
                        cardId = cardId,
                        cardName = cardName,
                        quantityOwned = 1,
                        quantityWanted = 0,
                        isCustom = isCustom
                    )
                )
            }
        }
    }

    /**
     * Add card to wishlist
     */
    fun addToWishlist(cardId: String, cardName: String, isCustom: Boolean = false) {
        viewModelScope.launch {
            val existing = repository.getUserCard(cardId)
            if (existing != null) {
                // Increment wanted count
                repository.addOrUpdateCard(
                    existing.copy(quantityWanted = existing.quantityWanted + 1)
                )
            } else {
                // Create new entry
                repository.addOrUpdateCard(
                    UserCard(
                        cardId = cardId,
                        cardName = cardName,
                        quantityOwned = 0,
                        quantityWanted = 1,
                        isCustom = isCustom
                    )
                )
            }
        }
    }

    /**
     * Update card quantities
     */
    fun updateCardQuantities(card: UserCard, owned: Int, wanted: Int) {
        viewModelScope.launch {
            if (owned <= 0 && wanted <= 0) {
                repository.deleteCard(card)
            } else {
                repository.addOrUpdateCard(
                    card.copy(quantityOwned = owned, quantityWanted = wanted)
                )
            }
        }
    }

    /**
     * Search for card names
     */
    fun searchCardNames(query: String) {
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    _searchSuggestions.value = emptyList()
                } else {
                    _searchSuggestions.value = repository.searchCardNames(query)
                }
            } catch (e: Exception) {
                android.util.Log.e("CardViewModel", "Search error: ${e.message}", e)
                _searchSuggestions.value = emptyList()
            }
        }
    }

    /**
     * Add card by name
     */
    fun addCardByName(cardName: String, toWishlist: Boolean = false) {
        viewModelScope.launch {
            // Create card ID from name and timestamp
            val cardId = "card_${cardName.replace(" ", "_")}_${System.currentTimeMillis()}"

            if (toWishlist) {
                addToWishlist(cardId, cardName, isCustom = false)
            } else {
                addToCollection(cardId, cardName, isCustom = false)
            }
        }
    }
}
