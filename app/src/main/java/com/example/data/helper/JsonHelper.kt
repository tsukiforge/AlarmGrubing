package com.example.data.helper

import com.example.data.model.Group
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object JsonHelper {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val groupAdapter = moshi.adapter(Group::class.java)

    fun groupToJson(group: Group): String {
        return groupAdapter.toJson(group)
    }

    fun jsonToGroup(json: String): Group? {
        return try {
            groupAdapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
