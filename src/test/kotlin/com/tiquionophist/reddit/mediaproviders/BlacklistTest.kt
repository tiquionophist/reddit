package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.Config
import com.tiquionophist.reddit.Media
import com.tiquionophist.reddit.MediaProvider
import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import okhttp3.HttpUrl
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class BlacklistTest {

    companion object {

        @BeforeAll
        @JvmStatic
        fun setup() {
            Config.load()
        }

        private val metadata = Media.Metadata(id = "id", author = "author", date = null, title = null)

        private val ignoredUrls = setOf(
            "https://sugarcookie.com/some/path",
            "https://sugarcookie.xxx/some/path",
            "https://harrietsugarcookie.com/some/path",
            "https://subdomain.sugarcookie.com/some/path",
            "https://reddit.com/r/all",
            "http://reddit.com/r/all",
            "https://www.reddit.com/r/all",
            "https://reddit.com/r/some_subRedditName"
        )

        private val notFoundUrls = setOf(
            "http://i.imgur.com/removed.png",
            "https://i.imgur.com/removed.png"
        )
    }

    @Test
    fun testMatches() {
        ignoredUrls.forEach { Blacklist.assertMatches(it) }
        notFoundUrls.forEach { Blacklist.assertMatches(it) }

        Blacklist.assertDoesNotMatch("https://imgur.com/abc.png")
        Blacklist.assertDoesNotMatch("https://imgur.com/abc")
        Blacklist.assertDoesNotMatch("https://imgur.com/a/abc")
        Blacklist.assertDoesNotMatch("https://gfycat.com/abc")
        Blacklist.assertDoesNotMatch("https://reddit.com/abc")
        Blacklist.assertDoesNotMatch("https://reddit.com/")
        Blacklist.assertDoesNotMatch("https://reddit.com/u/abc")
    }

    @Test
    fun testIgnoredUrls() {
        ignoredUrls.forEach { urlString ->
            val url = requireNotNull(HttpUrl.parse(urlString))
            assert(Blacklist.resolveMedia(metadata, url) == MediaProvider.Result.Ignored)
        }
    }

    @Test
    fun testNotFoundUrls() {
        notFoundUrls.forEach { urlString ->
            val url = requireNotNull(HttpUrl.parse(urlString))
            assert(Blacklist.resolveMedia(metadata, url) == MediaProvider.Result.NotFound)
        }
    }
}
