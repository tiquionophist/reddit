package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.UrlPermuter
import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class GfycatTest {
    @Test
    fun testMatches() {
        urls.permute(validIds).forEach { Gfycat.assertMatches(it) }
        urls.permute(validIds.map { "en/$it" }).forEach { Gfycat.assertMatches(it) }
        urls.permute(validIds.map { "gifs/detail/$it" }).forEach { Gfycat.assertMatches(it) }
        urls.permute(invalidPaths).forEach { Gfycat.assertDoesNotMatch(it) }
    }

    companion object {
        private val urls = UrlPermuter(host = "gfycat.com")

        private val validIds = listOf("abcd", "AbCd", "ab-cd", "abcd?1#2")
        private val invalidPaths = listOf("", " ", "123", "a1b2c3", "1+1=2", "a b", "a/b/c")
    }
}
