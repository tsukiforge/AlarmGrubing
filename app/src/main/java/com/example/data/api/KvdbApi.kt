package com.example.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface KvdbApi {
    @GET
    suspend fun getValue(
        @Url url: String
    ): Response<ResponseBody>

    @PUT
    suspend fun putValue(
        @Url url: String,
        @Body value: RequestBody
    ): Response<ResponseBody>
}
