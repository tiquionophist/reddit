package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.UrlPermuter
import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class RedditTest {

    companion object {

        private val urls = UrlPermuter(host = "i.reddituploads.com", allowWww = false)
    }

    @Test
    fun testMatches() {
        urls.permute(listOf("", "any1hing?1#2")).forEach { Reddit.assertMatches(it) }

        Reddit.assertDoesNotMatch("https://reddit.com/")
        Reddit.assertDoesNotMatch("https://reddit.com/abc")
        Reddit.assertDoesNotMatch("https://reddit.com/r/abc")
        Reddit.assertDoesNotMatch("https://reddit.com/u/abc")
        Reddit.assertDoesNotMatch("https://external-preview.redd.it/abc")
        Reddit.assertDoesNotMatch("http://v.redd.it/abc")
    }
}
