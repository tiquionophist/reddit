package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import okhttp3.HttpUrl

object Reddit : MediaProvider {
    private val hosts = setOf("i.reddituploads.com")

    override fun matches(url: HttpUrl) = hosts.contains(url.host())

    override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
        return MediaProvider.Result.Success(
            media = Media.File(metadata = metadata, urls = listOf(url))
        )
    }
}
