package com.tiquionophist.reddit

import com.tiquionophist.reddit.mediaproviders.*
import okhttp3.HttpUrl
import java.util.*

sealed class Media {

    data class Metadata(
        val id: String,
        val date: Date?,
        val title: String?,
        val position: Int? = null
    )

    abstract val metadata: Metadata

    data class File(
        override val metadata: Metadata,
        val urls: List<HttpUrl>
    ) : Media()

    data class Album(
        override val metadata: Metadata,
        val children: List<Media>
    ) : Media()
}

interface MediaProvider {

    sealed class Result {
        class Success(val media: Media) : Result()
        class Error(val error: Throwable) : Result()
        object NotFound : Result()
        object Ignored : Result()
    }

    fun matches(url: HttpUrl): Boolean

    fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): Result
}

// TODO move this somewhere else
val mediaProviders: List<MediaProvider> = listOf(
    IgnoredList,
    DirectLink,
    Reddit,
    Imgur.Image,
    Imgur.Album,
    Gfycat
)
