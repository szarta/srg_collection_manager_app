package com.srg.inventory.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.*

/**
 * Retrofit API service for get-diced.com
 */
interface GetDicedApi {

    @GET("cards")
    suspend fun searchCards(
        @Query("q") query: String? = null,
        @Query("card_type") cardType: String? = null,
        @Query("atk_type") atkType: String? = null,
        @Query("play_order") playOrder: String? = null,
        @Query("division") division: String? = null,
        @Query("gender") gender: String? = null,
        @Query("is_banned") isBanned: Boolean? = null,
        @Query("release_set") releaseSet: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): PaginatedCardResponse

    @GET("cards/{uuid}")
    suspend fun getCard(@Path("uuid") uuid: String): CardDto

    @GET("cards/slug/{slug}")
    suspend fun getCardBySlug(@Path("slug") slug: String): CardDto

    @POST("cards/by-uuids")
    suspend fun getCardsByUuids(@Body request: CardBatchRequest): CardBatchResponse

    @POST("api/shared-lists")
    suspend fun createSharedList(@Body request: SharedListRequest): SharedListCreateResponse

    @GET("api/shared-lists/{shared_id}")
    suspend fun getSharedList(@Path("shared_id") sharedId: String): SharedListResponse

    @DELETE("api/shared-lists/{shared_id}")
    suspend fun deleteSharedList(@Path("shared_id") sharedId: String)

    @GET("api/images/manifest")
    suspend fun getImageManifest(): ImageManifest

    @GET("api/cards/manifest")
    suspend fun getCardsManifest(): CardsManifest

    companion object {
        const val BASE_URL = "https://get-diced.com/"
    }
}

/**
 * Cards database manifest for sync
 */
data class CardsManifest(
    @SerializedName("version")
    val version: Int,
    @SerializedName("generated")
    val generated: String,
    @SerializedName("filename")
    val filename: String,
    @SerializedName("hash")
    val hash: String,
    @SerializedName("size_bytes")
    val size_bytes: Long,
    @SerializedName("card_count")
    val card_count: Int,
    @SerializedName("related_finishes_count")
    val related_finishes_count: Int,
    @SerializedName("related_cards_count")
    val related_cards_count: Int
)

/**
 * Image manifest for sync
 */
data class ImageManifest(
    @SerializedName("version")
    val version: Int,
    @SerializedName("generated")
    val generated: String,
    @SerializedName("image_count")
    val image_count: Int,
    @SerializedName("images")
    val images: Map<String, ImageInfo>
)

data class ImageInfo(
    @SerializedName("path")
    val path: String,
    @SerializedName("hash")
    val hash: String
)
