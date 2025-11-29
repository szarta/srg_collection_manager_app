package com.srg.inventory.api

import com.google.gson.annotations.SerializedName

/**
 * API response models for get-diced.com API
 */

data class PaginatedCardResponse(
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("items")
    val items: List<CardDto>
)

data class CardDto(
    @SerializedName("db_uuid")
    val dbUuid: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("card_type")
    val cardType: String,
    @SerializedName("rules_text")
    val rulesText: String? = null,
    @SerializedName("errata_text")
    val errataText: String? = null,
    @SerializedName("is_banned")
    val isBanned: Boolean = false,
    @SerializedName("release_set")
    val releaseSet: String? = null,
    @SerializedName("srg_url")
    val srgUrl: String? = null,
    @SerializedName("srgpc_url")
    val srgpcUrl: String? = null,
    @SerializedName("comments")
    val comments: String? = null,
    @SerializedName("tags")
    val tags: List<String>? = null,

    // Competitor fields
    @SerializedName("power")
    val power: Int? = null,
    @SerializedName("agility")
    val agility: Int? = null,
    @SerializedName("strike")
    val strike: Int? = null,
    @SerializedName("submission")
    val submission: Int? = null,
    @SerializedName("grapple")
    val grapple: Int? = null,
    @SerializedName("technique")
    val technique: Int? = null,
    @SerializedName("division")
    val division: String? = null,
    @SerializedName("gender")
    val gender: String? = null,

    // MainDeck fields
    @SerializedName("deck_card_number")
    val deckCardNumber: Int? = null,
    @SerializedName("atk_type")
    val atkType: String? = null,
    @SerializedName("play_order")
    val playOrder: String? = null
)

data class CardBatchRequest(
    @SerializedName("uuids")
    val uuids: List<String>
)

data class CardBatchResponse(
    @SerializedName("rows")
    val rows: List<CardDto>,
    @SerializedName("missing")
    val missing: List<String>
)

data class SharedListRequest(
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("card_uuids")
    val cardUuids: List<String>,
    @SerializedName("list_type")
    val listType: String = "COLLECTION",
    @SerializedName("deck_data")
    val deckData: DeckData? = null
)

data class SharedListResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("card_uuids")
    val cardUuids: List<String>,
    @SerializedName("list_type")
    val listType: String,
    @SerializedName("deck_data")
    val deckData: DeckData? = null,
    @SerializedName("created_at")
    val createdAt: String? = null
)

data class SharedListCreateResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("message")
    val message: String
)

data class DeckData(
    @SerializedName("spectacle_type")
    val spectacleType: String,
    @SerializedName("slots")
    val slots: List<DeckSlot>
)

data class DeckSlot(
    @SerializedName("slot_type")
    val slotType: String,
    @SerializedName("slot_number")
    val slotNumber: Int,
    @SerializedName("card_uuid")
    val cardUuid: String
)
