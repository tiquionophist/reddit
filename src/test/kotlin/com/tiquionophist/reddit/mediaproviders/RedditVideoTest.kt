package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.UrlPermuter
import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class RedditVideoTest {
    @Test
    fun testMatches() {
        urls.permute(listOf("", "any1hing?1#2")).forEach { RedditImage.assertMatches(it) }

        RedditImage.assertDoesNotMatch("https://reddit.com/")
        RedditImage.assertDoesNotMatch("https://reddit.com/abc")
        RedditImage.assertDoesNotMatch("https://reddit.com/r/abc")
        RedditImage.assertDoesNotMatch("https://reddit.com/u/abc")
        RedditImage.assertDoesNotMatch("https://external-preview.redd.it/abc")
        RedditImage.assertDoesNotMatch("https://i.reddituploads.com/abc")
    }

    companion object {
        private val urls = UrlPermuter(host = "v.redd.it", allowWww = false)
    }
}
