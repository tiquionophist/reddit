package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import okhttp3.HttpUrl

object RedditImage : MediaProvider {
    override fun matches(url: HttpUrl) = url.host() == "i.reddituploads.com"

    override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
        return MediaProvider.Result.Success(
            media = Media.File(metadata = metadata, urls = listOf(url))
        )
    }
}
