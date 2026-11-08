package com.example.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface HsStoreApiService {

    @GET
    suspend fun fetchRawJson(
        @Url url: String,
        @Header("Cache-Control") cacheControl: String = "no-cache"
    ): Response<ResponseBody>
}
