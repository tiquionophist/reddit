package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.UrlPermuter
import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class DirectLinkTest {
    @Test
    fun testMatches() {
        urls.permute(validPaths).forEach { DirectLink.assertMatches(it) }
        urls.permute(invalidPaths).forEach { DirectLink.assertDoesNotMatch(it) }
    }

    companion object {
        private val urls = UrlPermuter(host = "any.website", allowTrailingSlash = false)

        private val validPaths = listOf(
            "abcd.png",
            "abcd.jpeg",
            "abcd.png",
            "abcd.gif",
            "abcd.mp4",
            "abcd.webm",
            "abcd.png?1#2"
        )

        private val invalidPaths = listOf(
            "",
            " ",
            "abcd",
            "abcd.txt",
            "abcd.gifv",
            "abcd.png.nothing",
            "a1b2c3",
            "1+1=2",
            "a b"
        )
    }
}
