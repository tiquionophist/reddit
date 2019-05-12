package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import okhttp3.HttpUrl

object Youtube : MediaProvider {
    private val hosts = setOf("youtube.com", "www.youtube.com", "youtu.be")

    override fun matches(url: HttpUrl) = url.host() in hosts

    override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
        return MediaProvider.Result.Success(Media.Video(metadata = metadata, url = url))
    }
}
