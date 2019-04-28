package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import com.tiquionophist.reddit.satisfies
import okhttp3.HttpUrl

object IgnoredList : MediaProvider {

    // TODO move this list to a configuration file
    private val domains = setOf(
        "sugarcookie.com",
        "sugarcookie.xxx",
        "harrietsugarcookie.com"
    )

    private val subredditNameRegex = """\w+""".toRegex()

    override fun matches(url: HttpUrl): Boolean {
        return when {
            domains.contains(url.topPrivateDomain()) -> true
            url.isSubreddit() -> true
            else -> false
        }
    }

    private fun HttpUrl.isSubreddit(): Boolean {
        return topPrivateDomain() == "reddit.com" && pathSegments().satisfies(
            { it == "r" },
            { subredditNameRegex.matches(it.orEmpty()) },
            { it.isNullOrEmpty() }
        )
    }

    override fun resolveMedia(metadata: Media.Metadata, url: HttpUrl) = MediaProvider.Result.Ignored
}
