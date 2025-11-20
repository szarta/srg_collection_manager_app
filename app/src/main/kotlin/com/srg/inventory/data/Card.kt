package com.srg.inventory.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a card from the SRG database (synced from get-diced.com)
 * Supports all 7 card types with nullable fields for type-specific attributes
 */
@Entity(tableName = "cards")
data class Card(
    @PrimaryKey
    @ColumnInfo(name = "db_uuid")
    val dbUuid: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "card_type")
    val cardType: String,  // MainDeckCard, SingleCompetitorCard, TornadoCompetitorCard, etc.

    @ColumnInfo(name = "rules_text")
    val rulesText: String? = null,

    @ColumnInfo(name = "errata_text")
    val errataText: String? = null,

    @ColumnInfo(name = "is_banned")
    val isBanned: Boolean = false,

    @ColumnInfo(name = "release_set")
    val releaseSet: String? = null,

    @ColumnInfo(name = "srg_url")
    val srgUrl: String? = null,

    @ColumnInfo(name = "srgpc_url")
    val srgpcUrl: String? = null,

    @ColumnInfo(name = "comments")
    val comments: String? = null,

    // Tags stored as comma-separated string
    @ColumnInfo(name = "tags")
    val tags: String? = null,

    // Competitor-specific fields (nullable for non-competitor cards)
    @ColumnInfo(name = "power")
    val power: Int? = null,

    @ColumnInfo(name = "agility")
    val agility: Int? = null,

    @ColumnInfo(name = "strike")
    val strike: Int? = null,

    @ColumnInfo(name = "submission")
    val submission: Int? = null,

    @ColumnInfo(name = "grapple")
    val grapple: Int? = null,

    @ColumnInfo(name = "technique")
    val technique: Int? = null,

    @ColumnInfo(name = "division")
    val division: String? = null,

    @ColumnInfo(name = "gender")
    val gender: String? = null,  // For SingleCompetitorCard only

    // MainDeckCard-specific fields (nullable for non-main-deck cards)
    @ColumnInfo(name = "deck_card_number")
    val deckCardNumber: Int? = null,

    @ColumnInfo(name = "atk_type")
    val atkType: String? = null,  // Strike, Grapple, Submission

    @ColumnInfo(name = "play_order")
    val playOrder: String? = null,  // Lead, Followup, Finish

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long = System.currentTimeMillis()
) {
    // Helper properties
    val isCompetitor: Boolean
        get() = cardType.contains("Competitor")

    val isMainDeck: Boolean
        get() = cardType == "MainDeckCard"

    val tagList: List<String>
        get() = tags?.split(",")?.map { it.trim() } ?: emptyList()
}

/**
 * Entity for card finish relationships (e.g., foil variants)
 */
@Entity(
    tableName = "card_related_finishes",
    primaryKeys = ["card_uuid", "finish_uuid"],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Card::class,
            parentColumns = ["db_uuid"],
            childColumns = ["card_uuid"],
            onDelete = androidx.room.ForeignKey.CASCADE
        ),
        androidx.room.ForeignKey(
            entity = Card::class,
            parentColumns = ["db_uuid"],
            childColumns = ["finish_uuid"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("card_uuid"),
        androidx.room.Index("finish_uuid")
    ]
)
data class CardRelatedFinish(
    @ColumnInfo(name = "card_uuid")
    val cardUuid: String,

    @ColumnInfo(name = "finish_uuid")
    val finishUuid: String
)

/**
 * Entity for related card relationships
 */
@Entity(
    tableName = "card_related_cards",
    primaryKeys = ["card_uuid", "related_uuid"],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Card::class,
            parentColumns = ["db_uuid"],
            childColumns = ["card_uuid"],
            onDelete = androidx.room.ForeignKey.CASCADE
        ),
        androidx.room.ForeignKey(
            entity = Card::class,
            parentColumns = ["db_uuid"],
            childColumns = ["related_uuid"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index("card_uuid"),
        androidx.room.Index("related_uuid")
    ]
)
data class CardRelatedCard(
    @ColumnInfo(name = "card_uuid")
    val cardUuid: String,

    @ColumnInfo(name = "related_uuid")
    val relatedUuid: String
)
