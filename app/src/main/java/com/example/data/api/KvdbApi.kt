package com.example.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface KvdbApi {
    @GET("{bucketId}/objects/{key}")
    suspend fun getValue(
        @Path("bucketId") bucketId: String,
        @Path("key") key: String
    ): Response<ResponseBody>

    @PUT("{bucketId}/objects/{key}")
    suspend fun putValue(
        @Path("bucketId") bucketId: String,
        @Path("key") key: String,
        @Body value: RequestBody
    ): Response<ResponseBody>
}
