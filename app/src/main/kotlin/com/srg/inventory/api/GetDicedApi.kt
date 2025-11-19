package com.srg.inventory.api

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

    companion object {
        const val BASE_URL = "https://get-diced.com/"
    }
}
