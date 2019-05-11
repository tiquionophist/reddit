package com.tiquionophist.reddit.mediaproviders

import com.tiquionophist.reddit.UrlPermuter
import com.tiquionophist.reddit.assertDoesNotMatch
import com.tiquionophist.reddit.assertMatches
import org.junit.jupiter.api.Test

internal class ImgurTest {
    @Test
    fun testImageMatches() {
        urls.permute(validIds).forEach { Imgur.Image.assertMatches(it) }
        urls.permute(validIds.map { "a/$it" }).forEach { Imgur.Image.assertDoesNotMatch(it) }
        urls.permute(invalidPaths).forEach { Imgur.Image.assertDoesNotMatch(it) }
    }

    @Test
    fun testAlbumMatches() {
        urls.permute(validIds).forEach { Imgur.Album.assertDoesNotMatch(it) }
        urls.permute(validIds.map { "a/$it" }).forEach { Imgur.Album.assertMatches(it) }
        urls.permute(invalidPaths).forEach { Imgur.Album.assertDoesNotMatch(it) }
    }

    companion object {
        private val urls = UrlPermuter(host = "imgur.com", subdomains = listOf("m.", "i."))

        private val validIds = listOf("abcd", "AbCd", "123", "a1b2c3", "a1B2c3", "abcd?1#2", "abcd.gifv")
        private val invalidPaths = listOf("", " ", "1+1=2", "a b", "abcd.png", "abcd.mp4")
    }
}
