package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.UrlPermuter
import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class YoutubeTest {
    @Test
    fun testMatches() {
        mainUrls.permute(listOf("", "any1hing?1#2")).forEach { Youtube.assertMatches(it) }
        secondaryUrls.permute(listOf("", "any1hing?1#2")).forEach { Youtube.assertMatches(it) }

        Youtube.assertDoesNotMatch("https://reddit.com/abc")
        Youtube.assertDoesNotMatch("https://youtube.xxx/abc")
    }

    companion object {
        private val mainUrls = UrlPermuter(host = "youtube.com")
        private val secondaryUrls = UrlPermuter(host = "youtu.be", allowWww = false)
    }
}
