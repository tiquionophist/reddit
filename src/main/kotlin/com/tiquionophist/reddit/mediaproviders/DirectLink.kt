package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import okhttp3.HttpUrl

object DirectLink : MediaProvider {

    private val pathRegex = """.+\.(jpg|jpeg|png|gif|mp4|webm)""".toRegex()

    override fun matches(url: HttpUrl) = pathRegex.matches(url.encodedPath())

    override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
        return MediaProvider.Result.Success(
            media = Media.File(metadata = metadata, urls = listOf(url))
        )
    }
}
