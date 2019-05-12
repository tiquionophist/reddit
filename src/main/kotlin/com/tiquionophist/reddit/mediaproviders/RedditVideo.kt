package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import okhttp3.HttpUrl

object RedditVideo : MediaProvider {
    override fun matches(url: HttpUrl) = url.host() == "v.redd.it"

    override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
        return metadata.submission
            ?.embeddedMedia
            ?.redditVideo
            ?.fallbackUrl
            ?.let { HttpUrl.parse(it) }
            ?.let { MediaProvider.Result.Success(media = Media.File(metadata = metadata, urls = listOf(it))) }
            ?: MediaProvider.Result.NotFound
    }
}
