package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Config
import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import com.tiquionophist.reddit.satisfies
import okhttp3.HttpUrl

object Blacklist : MediaProvider {
    private val subredditNameRegex = """\w+""".toRegex()

    private fun HttpUrl.isSubreddit(): Boolean {
        return topPrivateDomain() == "reddit.com" && pathSegments().satisfies(
            { it == "r" },
            { subredditNameRegex.matches(it.orEmpty()) },
            { it.isNullOrEmpty() }
        )
    }

    private fun HttpUrl.isImgurRemovedUrl(): Boolean {
        return topPrivateDomain() == "imgur.com" && encodedPath() == "/removed.png"
    }

    override fun matches(url: HttpUrl): Boolean {
        return when {
            url.topPrivateDomain()?.let { Config.isIgnored(it) } == true -> true
            url.isSubreddit() -> true
            url.isImgurRemovedUrl() -> true
            else -> false
        }
    }

    override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl): MediaProvider.Result {
        return when {
            url.isImgurRemovedUrl() -> MediaProvider.Result.NotFound
            else -> MediaProvider.Result.Ignored
        }
    }
}
