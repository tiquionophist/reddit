package com.tiquionophist.reddit

import okhttp3.HttpUrl

interface MediaProvider {
    sealed class Result {
        data class Success(val media: Media) : Result()
        data class Error(val message: String, val cause: Throwable = Throwable(message)) : Result()
        object NotFound : Result()
        object Ignored : Result()
    }

    fun matches(url: HttpUrl): Boolean

    fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): Result
}
