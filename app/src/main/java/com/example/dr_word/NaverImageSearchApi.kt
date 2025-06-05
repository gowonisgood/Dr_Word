package com.example.dr_word

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NaverImageSearchApi {
    @GET("v1/search/image")
    suspend fun searchImages(
        @Query("query") query: String,
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String
    ): Response<NaverImageResponse>
}