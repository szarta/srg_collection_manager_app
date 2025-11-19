package com.srg.inventory.api

import com.srg.inventory.data.Card

/**
 * Maps CardDto from API to Card entity for database
 */
fun CardDto.toEntity(): Card {
    return Card(
        dbUuid = this.dbUuid,
        name = this.name,
        cardType = this.cardType,
        rulesText = this.rulesText,
        errataText = this.errataText,
        isBanned = this.isBanned,
        releaseSet = this.releaseSet,
        srgUrl = this.srgUrl,
        srgpcUrl = this.srgpcUrl,
        comments = this.comments,
        tags = this.tags?.joinToString(","),
        power = this.power,
        agility = this.agility,
        strike = this.strike,
        submission = this.submission,
        grapple = this.grapple,
        technique = this.technique,
        division = this.division,
        gender = this.gender,
        deckCardNumber = this.deckCardNumber,
        atkType = this.atkType,
        playOrder = this.playOrder,
        syncedAt = System.currentTimeMillis()
    )
}

fun List<CardDto>.toEntities(): List<Card> {
    return this.map { it.toEntity() }
}
