package com.example.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object ManifestModelsAdapterHelper {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val manifestAdapter = moshi.adapter(ProjectManifest::class.java).indent("  ")

    fun manifestToJson(manifest: ProjectManifest): String {
        return manifestAdapter.toJson(manifest)
    }

    fun manifestFromJson(json: String): ProjectManifest? {
        return manifestAdapter.fromJson(json)
    }
}
