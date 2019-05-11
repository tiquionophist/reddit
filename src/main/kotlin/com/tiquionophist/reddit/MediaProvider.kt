package com.tiquionophist.reddit

import com.tiquionophist.reddit.mediaproviders.Blacklist
import com.tiquionophist.reddit.mediaproviders.DirectLink
import com.tiquionophist.reddit.mediaproviders.Gfycat
import com.tiquionophist.reddit.mediaproviders.Imgur
import com.tiquionophist.reddit.mediaproviders.Reddit
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

// TODO move this somewhere else
val mediaProviders: List<MediaProvider> = listOf(
    Blacklist,
    DirectLink,
    Reddit,
    Imgur.Image,
    Imgur.Album,
    Gfycat
)
