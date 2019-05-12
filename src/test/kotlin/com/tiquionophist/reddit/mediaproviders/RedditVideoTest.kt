package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.UrlPermuter
import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class RedditVideoTest {
    @Test
    fun testMatches() {
        urls.permute(listOf("", "any1hing?1#2")).forEach { RedditVideo.assertMatches(it) }

        RedditVideo.assertDoesNotMatch("https://reddit.com/")
        RedditVideo.assertDoesNotMatch("https://reddit.com/abc")
        RedditVideo.assertDoesNotMatch("https://reddit.com/r/abc")
        RedditVideo.assertDoesNotMatch("https://reddit.com/u/abc")
        RedditVideo.assertDoesNotMatch("https://external-preview.redd.it/abc")
        RedditVideo.assertDoesNotMatch("https://i.reddituploads.com/abc")
    }

    companion object {
        private val urls = UrlPermuter(host = "v.redd.it", allowWww = false)
    }
}
